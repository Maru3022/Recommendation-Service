import FeedService from './FeedService';
import { api } from './ApiClient';

jest.mock('./ApiClient', () => ({
  api: { get: jest.fn(), post: jest.fn(), delete: jest.fn() },
}));

beforeEach(() => jest.clearAllMocks());

test.each([
  ['getPersonalizedFeed', '/api/v1/feed/personalized'],
  ['getFollowingFeed', '/api/v1/feed/following'],
])('%s uses the gateway contract', async (method, path) => {
  api.get.mockResolvedValue({ data: { posts: [{ postId: 'p1', text: 'Workout', score: 0.5 }], hasMore: true } });
  const result = await FeedService[method]('user1', 2, 10);
  expect(api.get).toHaveBeenCalledWith(path, { params: { userId: 'user1', page: 2, size: 10 } });
  expect(result).toEqual({ posts: [{ post: { id: 'p1', text: 'Workout', score: 0.5 }, score: 0.5 }], hasNext: true });
});

test('trending unwraps the backend page', async () => {
  api.get.mockResolvedValue({ data: { posts: [{ postId: 'p1' }], hasMore: false } });
  expect(await FeedService.getTrendingPosts(20)).toEqual([{ id: 'p1' }]);
  expect(api.get).toHaveBeenCalledWith('/api/v1/feed/trending', { params: { size: 20 } });
});

test('post creation uses the authenticated v1 endpoint', async () => {
  api.post.mockResolvedValue({ data: { postId: 'p2' } });
  await FeedService.createPost({ text: 'Done' });
  expect(api.post).toHaveBeenCalledWith('/api/v1/posts', { text: 'Done' });
});
