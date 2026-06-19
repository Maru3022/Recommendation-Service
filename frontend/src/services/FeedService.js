import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8026';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
});

class FeedService {
  // Feed endpoints
  static async getPersonalizedFeed(userId, page = 0, size = 10) {
    const { data } = await api.get(`/api/feed/${userId}`, { params: { page, size } });
    return data;
  }

  static async getFollowingFeed(userId, page = 0, size = 20) {
    const { data } = await api.get(`/api/feed/${userId}/following`, { params: { page, size } });
    return data;
  }

  static async getTrendingPosts(limit = 20) {
    const { data } = await api.get('/api/feed/trending', { params: { limit } });
    return data;
  }

  static async searchPosts(userId, query) {
    const { data } = await api.post('/api/feed/search', { userId, query });
    return data;
  }

  // Post endpoints
  static async createPost(postData) {
    const { data } = await api.post('/api/posts', postData);
    return data;
  }

  static async getPost(postId) {
    const { data } = await api.get(`/api/posts/${postId}`);
    return data;
  }

  static async deletePost(postId, requestingUserId) {
    await api.delete(`/api/posts/${postId}`, { params: { requestingUserId } });
  }

  // Action tracking (fire-and-forget style)
  static async trackAction(postId, userId, actionType) {
    try {
      await api.post(`/api/posts/${postId}/actions`, { userId, actionType });
    } catch (err) {
      console.warn('Failed to track action:', err?.message);
    }
  }

  static async getUserHistory(userId) {
    const { data } = await api.get(`/api/posts/${userId}/history`);
    return data;
  }

  // Social graph
  static async follow(userId, targetId) {
    await api.post(`/api/social/${userId}/follow/${targetId}`);
  }

  static async unfollow(userId, targetId) {
    await api.delete(`/api/social/${userId}/follow/${targetId}`);
  }

  static async getFollowing(userId) {
    const { data } = await api.get(`/api/social/${userId}/following`);
    return data;
  }
}

export default FeedService;
