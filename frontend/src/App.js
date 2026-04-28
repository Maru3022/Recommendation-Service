import React, { useState, useEffect } from 'react';
import { Search, User, TrendingUp, Package, Heart, ShoppingCart, Star } from 'lucide-react';
import RecommendationService from './services/RecommendationService';
import ProductCard from './components/ProductCard';
import UserSelector from './components/UserSelector';
import LoadingSpinner from './components/LoadingSpinner';

function App() {
  const [user, setUser] = useState('user1');
  const [recommendations, setRecommendations] = useState([]);
  const [popularProducts, setPopularProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('personalized');
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);

  useEffect(() => {
    loadRecommendations();
  }, [user, page]);

  useEffect(() => {
    if (activeTab === 'popular') {
      loadPopularProducts();
    }
  }, [activeTab]);

  const loadRecommendations = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await RecommendationService.getRecommendations(user, page, 10);
      setRecommendations(response.products);
      setHasNext(response.hasNext);
    } catch (err) {
      setError('Failed to load recommendations. Please try again.');
      console.error('Error loading recommendations:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadPopularProducts = async () => {
    setLoading(true);
    setError(null);
    try {
      const products = await RecommendationService.getPopularProducts(20);
      setPopularProducts(products);
    } catch (err) {
      setError('Failed to load popular products. Please try again.');
      console.error('Error loading popular products:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleUserChange = (newUser) => {
    setUser(newUser);
    setPage(0);
  };

  const handleNextPage = () => {
    if (hasNext) {
      setPage(page + 1);
    }
  };

  const handlePrevPage = () => {
    if (page > 0) {
      setPage(page - 1);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
      {/* Header */}
      <header className="bg-white shadow-lg">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <Package className="w-8 h-8 text-blue-600" />
              <h1 className="text-3xl font-bold text-gray-900">Recommendation Service</h1>
            </div>
            <UserSelector 
              selectedUser={user} 
              onUserChange={handleUserChange} 
            />
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Tab Navigation */}
        <div className="bg-white rounded-lg shadow-md mb-8">
          <div className="border-b border-gray-200">
            <nav className="flex -mb-px">
              <button
                onClick={() => setActiveTab('personalized')}
                className={`py-4 px-6 text-sm font-medium border-b-2 transition-colors ${
                  activeTab === 'personalized'
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <div className="flex items-center space-x-2">
                  <User className="w-4 h-4" />
                  <span>Personalized for {user}</span>
                </div>
              </button>
              <button
                onClick={() => setActiveTab('popular')}
                className={`py-4 px-6 text-sm font-medium border-b-2 transition-colors ${
                  activeTab === 'popular'
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <div className="flex items-center space-x-2">
                  <TrendingUp className="w-4 h-4" />
                  <span>Popular Products</span>
                </div>
              </button>
            </nav>
          </div>
        </div>

        {/* Error Message */}
        {error && (
          <div className="bg-red-50 border-l-4 border-red-400 p-4 mb-6">
            <div className="flex">
              <div className="ml-3">
                <p className="text-sm text-red-700">{error}</p>
              </div>
            </div>
          </div>
        )}

        {/* Loading State */}
        {loading && <LoadingSpinner />}

        {/* Content */}
        {!loading && (
          <div>
            {activeTab === 'personalized' ? (
              <div>
                <div className="flex justify-between items-center mb-6">
                  <h2 className="text-2xl font-semibold text-gray-800">
                    Recommended for {user}
                  </h2>
                  <div className="flex items-center space-x-2">
                    <button
                      onClick={handlePrevPage}
                      disabled={page === 0}
                      className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-300 transition-colors"
                    >
                      Previous
                    </button>
                    <span className="text-gray-600">
                      Page {page + 1}
                    </span>
                    <button
                      onClick={handleNextPage}
                      disabled={!hasNext}
                      className="px-4 py-2 bg-gray-200 text-gray-700 rounded-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-300 transition-colors"
                    >
                      Next
                    </button>
                  </div>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                  {recommendations.map((product) => (
                    <ProductCard key={product.id} product={product} />
                  ))}
                </div>
                {recommendations.length === 0 && (
                  <div className="text-center py-12">
                    <Package className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                    <p className="text-gray-500 text-lg">No recommendations available</p>
                  </div>
                )}
              </div>
            ) : (
              <div>
                <h2 className="text-2xl font-semibold text-gray-800 mb-6">
                  Popular Products
                </h2>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                  {popularProducts.map((product) => (
                    <ProductCard key={product.id} product={product} />
                  ))}
                </div>
                {popularProducts.length === 0 && (
                  <div className="text-center py-12">
                    <TrendingUp className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                    <p className="text-gray-500 text-lg">No popular products available</p>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  );
}

export default App;
