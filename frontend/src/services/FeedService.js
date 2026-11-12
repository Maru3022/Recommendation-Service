import { api } from './ApiClient';

const toPost = ({ postId, ...post }) => ({ ...post, id: postId });
const toFeed = (data) => ({
  posts: (data.posts || []).map(post => ({ post: toPost(post), score: post.score })),
  hasNext: Boolean(data.hasMore),
});

class FeedService {
  // Feed endpoints
  static async getPersonalizedFeed(userId, page = 0, size = 10) {
    const { data } = await api.get('/api/v1/feed/personalized', { params: { userId, page, size } });
    return toFeed(data);
  }

  static async getFollowingFeed(userId, page = 0, size = 20) {
    const { data } = await api.get('/api/v1/feed/following', { params: { userId, page, size } });
    return toFeed(data);
  }

  static async getTrendingPosts(limit = 20) {
    const { data } = await api.get('/api/v1/feed/trending', { params: { size: limit } });
    return (data.posts || []).map(toPost);
  }

  static async searchPosts(userId, query) {
    const { data } = await api.post('/api/v1/search/search', { userId, query });
    return data;
  }

  // Post endpoints
  static async createPost(postData) {
    const { data } = await api.post('/api/v1/posts', postData);
    return data;
  }

  static async getPost(postId) {
    const { data } = await api.get(`/api/v1/posts/${postId}`);
    return data;
  }

  static async deletePost(postId, requestingUserId) {
    await api.delete(`/api/v1/posts/${postId}`, { params: { requestingUserId } });
  }

  // Action tracking (fire-and-forget style)
  static async trackAction(postId, userId, actionType) {
    try {
      await api.post(`/api/v1/posts/${postId}/actions`, { userId, actionType });
    } catch (err) {
      console.warn('Failed to track action:', err?.message);
    }
  }

  static async getUserHistory(userId) {
    const { data } = await api.get('/api/v1/posts/history', { params: { userId } });
    return data;
  }

  // Social graph
  static async follow(userId, targetId) {
    await api.post(`/api/v1/social/follow/${targetId}`, null, { params: { userId } });
  }

  static async unfollow(userId, targetId) {
    await api.delete(`/api/v1/social/follow/${targetId}`, { params: { userId } });
  }

  static async getFollowing(userId) {
    const { data } = await api.get('/api/v1/social/following', { params: { userId } });
    return data;
  }
}

export default FeedService;
