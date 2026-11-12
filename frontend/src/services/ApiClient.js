import axios from 'axios';

const TOKEN_KEY = 'fitflow.auth';
const baseURL = process.env.REACT_APP_API_URL || 'http://localhost:8075';

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

api.interceptors.request.use((config) => {
  const auth = getStoredAuth();
  if (auth?.accessToken) config.headers.Authorization = `Bearer ${auth.accessToken}`;
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error.response?.data?.message
      || error.response?.data?.error
      || (error.code === 'ECONNABORTED' ? 'Сервис не ответил вовремя' : error.message)
      || 'Не удалось выполнить запрос';
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
  async trainings() { return (await api.get('/api/v1/trainings')).data; },
  async createTraining(payload) { return (await api.post('/api/v1/trainings', payload)).data; },
  async deleteTraining(id) { await api.delete(`/api/v1/trainings/${id}`); },
  async mealLogs() { return (await api.get('/api/v1/meal-logs')).data; },
  async createMeal(payload) { return (await api.post('/api/v1/meal-logs', payload)).data; },
  async tips() { return (await api.get('/api/v1/tips')).data; },
  async advice() { return (await api.get('/api/v1/advice')).data; },
};

