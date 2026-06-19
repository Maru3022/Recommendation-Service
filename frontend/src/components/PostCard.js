import React, { useState } from 'react';
import { Heart, MessageCircle, Share2, Bookmark, Clock, Flame } from 'lucide-react';
import FeedService from '../services/FeedService';

const POST_TYPE_LABELS = {
  WORKOUT_COMPLETED: '🏋️ Workout',
  ACHIEVEMENT:       '🏆 Achievement',
  PROGRESS_PHOTO:    '📸 Progress',
  TIP:               '💡 Tip',
  MEAL_LOG:          '🥗 Meal',
  FREEFORM:          '✍️ Post',
};

const PostCard = ({ post, currentUserId }) => {
  const [liked, setLiked]   = useState(false);
  const [saved, setSaved]   = useState(false);
  const [likes, setLikes]   = useState(post.likesCount || 0);

  const handleLike = () => {
    const next = !liked;
    setLiked(next);
    setLikes(prev => prev + (next ? 1 : -1));
    FeedService.trackAction(post.id, currentUserId, next ? 'LIKE' : 'VIEW');
  };

  const handleSave = () => {
    const next = !saved;
    setSaved(next);
    FeedService.trackAction(post.id, currentUserId, next ? 'SAVE' : 'VIEW');
  };

  const handleShare = () => {
    FeedService.trackAction(post.id, currentUserId, 'SHARE');
    navigator.clipboard?.writeText(window.location.origin + '/posts/' + post.id)
      .catch(() => {});
  };

  const formatDate = (iso) => {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  };

  const typeLabel = POST_TYPE_LABELS[post.postType] || '✍️ Post';
  const avatarUrl = post.authorAvatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(post.authorDisplayName || 'U')}&background=random`;

  return (
    <article
      className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-md transition-shadow"
      aria-label={`Post by ${post.authorDisplayName}`}
    >
      {/* Header */}
      <div className="flex items-center justify-between px-4 pt-4 pb-2">
        <div className="flex items-center space-x-3">
          <img
            src={avatarUrl}
            alt={post.authorDisplayName}
            className="w-10 h-10 rounded-full object-cover"
          />
          <div>
            <p className="font-semibold text-gray-900 text-sm">{post.authorDisplayName || 'Unknown'}</p>
            <p className="text-xs text-gray-500">{formatDate(post.createdAt)}</p>
          </div>
        </div>
        <span className="text-xs font-medium px-2 py-1 rounded-full bg-indigo-50 text-indigo-700">
          {typeLabel}
        </span>
      </div>

      {/* Media */}
      {post.mediaUrls && post.mediaUrls.length > 0 && (
        <div className="relative bg-gray-100">
          <img
            src={post.mediaUrls[0]}
            alt="Post media"
            className="w-full max-h-72 object-cover"
            onError={(e) => { e.target.style.display = 'none'; }}
          />
        </div>
      )}

      {/* Body */}
      <div className="px-4 py-3">
        {post.text && (
          <p className="text-gray-800 text-sm leading-relaxed mb-2 line-clamp-3">{post.text}</p>
        )}

        {/* Stats row */}
        {(post.durationMinutes || post.caloriesBurned) && (
          <div className="flex items-center space-x-4 mb-2">
            {post.durationMinutes && (
              <span className="flex items-center space-x-1 text-xs text-gray-500">
                <Clock className="w-3.5 h-3.5" aria-hidden="true" />
                <span>{post.durationMinutes} min</span>
              </span>
            )}
            {post.caloriesBurned && (
              <span className="flex items-center space-x-1 text-xs text-gray-500">
                <Flame className="w-3.5 h-3.5" aria-hidden="true" />
                <span>{post.caloriesBurned} kcal</span>
              </span>
            )}
          </div>
        )}

        {/* Tags */}
        {post.tags && post.tags.length > 0 && (
          <div className="flex flex-wrap gap-1 mb-3">
            {post.tags.slice(0, 5).map(tag => (
              <span key={tag} className="text-xs text-blue-600 font-medium">#{tag}</span>
            ))}
          </div>
        )}
      </div>

      {/* Actions */}
      <div className="flex items-center justify-between px-4 pb-4 border-t border-gray-50 pt-3">
        <div className="flex items-center space-x-4">
          <button
            onClick={handleLike}
            className={`flex items-center space-x-1.5 text-sm transition-colors ${liked ? 'text-red-500' : 'text-gray-500 hover:text-red-400'}`}
            aria-label={liked ? 'Unlike post' : 'Like post'}
            aria-pressed={liked}
          >
            <Heart className={`w-5 h-5 ${liked ? 'fill-current' : ''}`} />
            <span>{likes}</span>
          </button>

          <button
            className="flex items-center space-x-1.5 text-sm text-gray-500 hover:text-blue-400 transition-colors"
            aria-label="Comment on post"
            onClick={() => FeedService.trackAction(post.id, currentUserId, 'COMMENT')}
          >
            <MessageCircle className="w-5 h-5" />
            <span>{post.commentsCount || 0}</span>
          </button>

          <button
            onClick={handleShare}
            className="flex items-center space-x-1.5 text-sm text-gray-500 hover:text-green-500 transition-colors"
            aria-label="Share post"
          >
            <Share2 className="w-5 h-5" />
          </button>
        </div>

        <button
          onClick={handleSave}
          className={`transition-colors ${saved ? 'text-yellow-500' : 'text-gray-400 hover:text-yellow-400'}`}
          aria-label={saved ? 'Unsave post' : 'Save post'}
          aria-pressed={saved}
        >
          <Bookmark className={`w-5 h-5 ${saved ? 'fill-current' : ''}`} />
        </button>
      </div>
    </article>
  );
};

export default PostCard;
