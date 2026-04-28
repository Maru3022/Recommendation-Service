import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8026';

class RecommendationService {
  static async getRecommendations(userId, page = 0, size = 10) {
    try {
      const response = await axios.get(
        `${API_BASE_URL}/api/recommendations/${userId}`,
        {
          params: { page, size },
          timeout: 10000,
        }
      );
      return response.data;
    } catch (error) {
      console.error('Error fetching recommendations:', error);
      throw error;
    }
  }

  static async getPopularProducts(limit = 20) {
    try {
      const response = await axios.get(
        `${API_BASE_URL}/api/recommendations/popular`,
        {
          params: { limit },
          timeout: 10000,
        }
      );
      return response.data;
    } catch (error) {
      console.error('Error fetching popular products:', error);
      throw error;
    }
  }

  static async trackUserAction(userId, productId, actionType) {
    try {
      await axios.post(
        `${API_BASE_URL}/api/user-actions`,
        {
          userId,
          productId,
          actionType,
        },
        {
          timeout: 5000,
        }
      );
    } catch (error) {
      console.error('Error tracking user action:', error);
      // Don't throw error for tracking - it's not critical
    }
  }
}

export default RecommendationService;
