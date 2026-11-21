import axios from 'axios';

const TOKEN_KEY = 'fitflow.auth';
const baseURL = process.env.REACT_APP_API_URL || '/';

export const api = axios.create({ baseURL, timeout: 15000 });

export const getStoredAuth = () => {
  try {
    return JSON.parse(localStorage.getItem(TOKEN_KEY)) || null;
  } catch {
    return null;
  }
};

export const storeAuth = (auth) => {
  if (auth) localStorage.setItem(TOKEN_KEY, JSON.stringify(auth));
  else localStorage.removeItem(TOKEN_KEY);
};

export const storeUser = (user) => {
  const auth = getStoredAuth();
  if (auth) storeAuth({ ...auth, user });
};

api.interceptors.request.use((config) => {
  const auth = getStoredAuth();
  if (auth?.accessToken) config.headers.Authorization = `Bearer ${auth.accessToken}`;
  return config;
});

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) prom.reject(error);
    else prom.resolve(token);
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url?.includes('/api/v1/auth/')) {
      const auth = getStoredAuth();
      if (auth?.refreshToken) {
        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            failedQueue.push({ resolve, reject });
          }).then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return api(originalRequest);
          });
        }
        originalRequest._retry = true;
        isRefreshing = true;
        try {
          const res = await axios.post(`${baseURL.replace(/\/$/, '')}/api/v1/auth/refresh`, {
            refreshToken: auth.refreshToken
          });
          const newAuth = res.data;
          storeAuth(newAuth);
          api.defaults.headers.common.Authorization = `Bearer ${newAuth.accessToken}`;
          originalRequest.headers.Authorization = `Bearer ${newAuth.accessToken}`;
          processQueue(null, newAuth.accessToken);
          return api(originalRequest);
        } catch (refreshErr) {
          processQueue(refreshErr, null);
          storeAuth(null);
          window.location.reload();
          return Promise.reject(new Error('Сессия истекла. Пожалуйста, войдите снова.'));
        } finally {
          isRefreshing = false;
        }
      } else {
        storeAuth(null);
        window.location.reload();
        return Promise.reject(new Error('Сессия истекла. Пожалуйста, войдите снова.'));
      }
    }
    const rawMessage = error.response?.data?.message
      || error.response?.data?.error
      || (typeof error.response?.data === 'string' ? error.response.data : '')
      || error.message;
    const message = /unexpected error|internal server error/i.test(rawMessage || '')
      ? 'Не удалось выполнить операцию. Проверьте введённые данные и попробуйте ещё раз.'
      : error.code === 'ECONNABORTED'
        ? 'Сервис не ответил вовремя. Попробуйте ещё раз.'
        : !error.response
          ? 'Нет связи с сервером. Проверьте, что локальное окружение запущено.'
          : rawMessage || 'Не удалось выполнить запрос';
    return Promise.reject(new Error(message));
  },
);

export const FitFlowApi = {
  async register(payload) {
    const { data } = await api.post('/api/v1/auth/register', payload);
    storeAuth(data);
    return data;
  },
  async login(payload) {
    const { data } = await api.post('/api/v1/auth/login', payload);
    storeAuth(data);
    return data;
  },
  async me() { return (await api.get('/api/v1/users/me')).data; },
  async updateProfile(user, changes) {
    const updated = (await api.put(`/api/v1/users/${user.id}`, { ...user, ...changes })).data;
    storeUser(updated);
    return updated;
  },
  async stats(userId) { return (await api.get(`/api/v1/users/${userId}/stats`)).data; },
  avatarUrl(userId) { return `${baseURL.replace(/\/$/, '')}/api/v1/users/${userId}/avatar`; },
  async uploadAvatar(file) { const body = new FormData(); body.append('file', file); await api.post('/api/v1/users/me/avatar', body); },
  async deleteAvatar() { await api.delete('/api/v1/users/me/avatar'); },
  async trainings() { return (await api.get('/api/v1/trainings', { params: { sort: 'createdAt,desc' } })).data; },
  async createTraining(payload) { return (await api.post('/api/v1/trainings', payload)).data; },
  async deleteTraining(id) { await api.delete(`/api/v1/trainings/${id}`); },
  async mealLogs() { return (await api.get('/api/v1/meal-logs')).data; },
  async createMeal(payload) { return (await api.post('/api/v1/meal-logs', payload)).data; },
  async tips() { return (await api.get('/api/v1/tips')).data; },
  async advice() { return (await api.get('/api/v1/advice')).data; },
};

