import React, { useState } from 'react';
import { Heart, ShoppingCart, Star, ExternalLink } from 'lucide-react';
import RecommendationService from '../services/RecommendationService';

const ProductCard = ({ product }) => {
  const [isLiked, setIsLiked] = useState(false);
  const [isInCart, setIsInCart] = useState(false);

  const handleLike = () => {
    setIsLiked(!isLiked);
    RecommendationService.trackUserAction('current-user', product.id, 'like');
  };

  const handleAddToCart = () => {
    setIsInCart(!isInCart);
    RecommendationService.trackUserAction('current-user', product.id, 'add_to_cart');
  };

  const formatPrice = (price) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(price);
  };

  const defaultImage = 'https://via.placeholder.com/300x200?text=Product+Image';

  return (
    <div className="bg-white rounded-lg shadow-md overflow-hidden product-card">
      {/* Product Image */}
      <div className="relative h-48 bg-gray-100">
        <img
          src={product.imageUrl || defaultImage}
          alt={product.name}
          className="w-full h-full object-cover"
          onError={(e) => {
            e.target.src = defaultImage;
          }}
        />
        <div className="absolute top-2 right-2">
          <button
            onClick={handleLike}
            className={`p-2 rounded-full transition-colors ${
              isLiked
                ? 'bg-red-500 text-white'
                : 'bg-white text-gray-600 hover:bg-gray-100'
            }`}
            aria-label="Like product"
          >
            <Heart className={`w-4 h-4 ${isLiked ? 'fill-current' : ''}`} />
          </button>
        </div>
      </div>

      {/* Product Info */}
      <div className="p-4">
        <div className="mb-2">
          <span className="inline-block px-2 py-1 text-xs font-semibold text-blue-600 bg-blue-100 rounded-full">
            {product.category}
          </span>
        </div>
        
        <h3 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-2">
          {product.name}
        </h3>
        
        <div className="flex items-center justify-between mb-3">
          <div className="text-2xl font-bold text-green-600">
            {formatPrice(product.price)}
          </div>
          <div className="flex items-center space-x-1">
            {[...Array(5)].map((_, i) => (
              <Star
                key={i}
                className={`w-4 h-4 ${
                  i < 4 ? 'text-yellow-400 fill-current' : 'text-gray-300'
                }`}
              />
            ))}
            <span className="text-sm text-gray-600 ml-1">(4.0)</span>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex space-x-2">
          <button
            onClick={handleAddToCart}
            className={`flex-1 flex items-center justify-center space-x-2 py-2 px-4 rounded-lg font-medium transition-colors ${
              isInCart
                ? 'bg-green-500 text-white'
                : 'bg-blue-500 text-white hover:bg-blue-600'
            }`}
          >
            <ShoppingCart className="w-4 h-4" />
            <span>{isInCart ? 'In Cart' : 'Add to Cart'}</span>
          </button>
          
          <button
            className="p-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            aria-label="View product details"
          >
            <ExternalLink className="w-4 h-4 text-gray-600" />
          </button>
        </div>
      </div>
    </div>
  );
};

export default ProductCard;
