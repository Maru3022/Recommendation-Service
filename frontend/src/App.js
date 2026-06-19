import React, { useState, useEffect, useCallback } from 'react';
import { Activity, TrendingUp, Users, Search, X } from 'lucide-react';
import FeedService from './services/FeedService';
import PostCard from './components/PostCard';
import UserSelector from './components/UserSelector';
import LoadingSpinner from './components/LoadingSpinner';

const TABS = [
  { id: 'personalized', label: 'For You',    icon: Activity  },
  { id: 'following',    label: 'Following',  icon: Users     },
  { id: 'trending',     label: 'Trending',   icon: TrendingUp },
];

export default function App() {
  const [userId,    setUserId]    = useState('user1');
  const [activeTab, setActiveTab] = useState('personalized');
  const [posts,     setPosts]     = useState([]);
  const [page,      setPage]      = useState(0);
  const [hasNext,   setHasNext]   = useState(false);
  const [loading,   setLoading]   = useState(false);
  const [error,     setError]     = useState(null);

  // Search state
  const [searchOpen,  setSearchOpen]  = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResult, setSearchResult] = useState(null);
  const [searching,   setSearching]   = useState(false);

  const loadFeed = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      let data;
      if (activeTab === 'personalized') {
        data = await FeedService.getPersonalizedFeed(userId, page, 10);
        setPosts(data.posts || []);
        setHasNext(data.hasNext || false);
      } else if (activeTab === 'following') {
        data = await FeedService.getFollowingFeed(userId, page, 20);
        setPosts(data.posts || []);
        setHasNext(data.hasNext || false);
      } else {
        const trending = await FeedService.getTrendingPosts(20);
        // Wrap plain PostDoc array as RankedPost for uniform rendering
        setPosts((trending || []).map(p => ({ post: p, score: 0 })));
        setHasNext(false);
      }
    } catch (err) {
      setError('Could not load feed. Is the backend running?');
      console.error(err);
    } finally {
      setLoading(false);
    }
  }, [userId, activeTab, page]);

  useEffect(() => { loadFeed(); }, [loadFeed]);

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setPage(0);
    setPosts([]);
    setSearchResult(null);
  };

  const handleUserChange = (id) => {
    setUserId(id);
    setPage(0);
    setPosts([]);
    setSearchResult(null);
  };

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;
    setSearching(true);
    setError(null);
    try {
      const result = await FeedService.searchPosts(userId, searchQuery);
      setSearchResult(result);
    } catch (err) {
      setError('Search failed. Please try again.');
    } finally {
      setSearching(false);
    }
  };

  // Normalise: both /trending (PostDoc[]) and /feed (RankedPost[]) come through as RankedPost
  const displayPosts = searchResult
    ? (searchResult.posts || []).map(p => ({ post: p, score: 0 }))
    : posts;

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-indigo-50">
      {/* ── Header ─────────────────────────────────────────────────────── */}
      <header className="bg-white shadow-sm sticky top-0 z-10">
        <div className="max-w-3xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Activity className="w-7 h-7 text-indigo-600" aria-hidden="true" />
            <h1 className="text-xl font-bold text-gray-900">FitFeed</h1>
          </div>

          <div className="flex items-center space-x-3">
            <button
              onClick={() => setSearchOpen(v => !v)}
              className="p-2 rounded-lg text-gray-500 hover:bg-gray-100 transition-colors"
              aria-label="Toggle search"
            >
              <Search className="w-5 h-5" />
            </button>
            <UserSelector selectedUser={userId} onUserChange={handleUserChange} />
          </div>
        </div>

        {/* Search bar */}
        {searchOpen && (
          <div className="border-t border-gray-100 bg-white px-4 py-3">
            <form onSubmit={handleSearch} className="flex space-x-2">
              <input
                type="text"
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                placeholder="Search posts by topic, e.g. 'leg day tips'…"
                className="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-400"
                aria-label="Search query"
              />
              <button
                type="submit"
                disabled={searching}
                className="bg-indigo-600 text-white px-4 py-2 rounded-lg text-sm font-medium disabled:opacity-50 hover:bg-indigo-700 transition-colors"
              >
                {searching ? '…' : 'Search'}
              </button>
              {searchResult && (
                <button
                  type="button"
                  onClick={() => { setSearchResult(null); setSearchQuery(''); }}
                  className="p-2 text-gray-500 hover:text-gray-700"
                  aria-label="Clear search"
                >
                  <X className="w-5 h-5" />
                </button>
              )}
            </form>
            {searchResult?.aiExplanation && (
              <p className="mt-2 text-xs text-indigo-700 bg-indigo-50 rounded-lg px-3 py-2">
                🤖 {searchResult.aiExplanation}
              </p>
            )}
          </div>
        )}
      </header>

      {/* ── Tab bar ────────────────────────────────────────────────────── */}
      {!searchResult && (
        <nav className="bg-white border-b border-gray-100 sticky top-[72px] z-10" aria-label="Feed tabs">
          <div className="max-w-3xl mx-auto px-4 flex">
            {TABS.map(({ id, label, icon: Icon }) => (
              <button
                key={id}
                onClick={() => handleTabChange(id)}
                className={`flex items-center space-x-1.5 py-3 px-5 text-sm font-medium border-b-2 transition-colors ${
                  activeTab === id
                    ? 'border-indigo-500 text-indigo-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700'
                }`}
                aria-selected={activeTab === id}
                role="tab"
              >
                <Icon className="w-4 h-4" aria-hidden="true" />
                <span>{label}</span>
              </button>
            ))}
          </div>
        </nav>
      )}

      {/* ── Main ───────────────────────────────────────────────────────── */}
      <main className="max-w-3xl mx-auto px-4 py-6">
        {/* Search heading */}
        {searchResult && (
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-gray-800">
              Search results for "{searchQuery}"
            </h2>
            <button
              onClick={() => { setSearchResult(null); setSearchQuery(''); }}
              className="text-sm text-indigo-600 hover:underline"
            >
              Back to feed
            </button>
          </div>
        )}

        {/* Error */}
        {error && (
          <div role="alert" className="bg-red-50 border-l-4 border-red-400 p-4 mb-4 rounded-r-lg text-sm text-red-700">
            {error}
          </div>
        )}

        {/* Loading */}
        {(loading || searching) && <LoadingSpinner />}

        {/* Posts */}
        {!loading && !searching && (
          <>
            <div className="space-y-4">
              {displayPosts.map(({ post }) => (
                <PostCard key={post.id} post={post} currentUserId={userId} />
              ))}
            </div>

            {displayPosts.length === 0 && (
              <div className="text-center py-16 text-gray-400">
                <Activity className="w-12 h-12 mx-auto mb-3 opacity-40" />
                <p className="text-lg">No posts yet</p>
                <p className="text-sm mt-1">
                  {activeTab === 'following'
                    ? 'Follow some users to see their posts here.'
                    : 'Check back soon for fresh content.'}
                </p>
              </div>
            )}

            {/* Pagination — only for paginated tabs */}
            {!searchResult && activeTab !== 'trending' && (
              <div className="flex justify-center items-center space-x-4 mt-8">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-4 py-2 bg-white border border-gray-300 rounded-lg text-sm disabled:opacity-40 hover:bg-gray-50 transition-colors"
                >
                  Previous
                </button>
                <span className="text-sm text-gray-600">Page {page + 1}</span>
                <button
                  onClick={() => setPage(p => p + 1)}
                  disabled={!hasNext}
                  className="px-4 py-2 bg-white border border-gray-300 rounded-lg text-sm disabled:opacity-40 hover:bg-gray-50 transition-colors"
                >
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}
