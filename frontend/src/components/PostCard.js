import React, { useState } from 'react';
import { Bookmark, Flame, Heart, Share2, Clock } from 'lucide-react';
import FeedService from '../services/FeedService';

const labels = {
  WORKOUT_COMPLETED: 'Тренировка', ACHIEVEMENT: 'Достижение',
  PROGRESS_PHOTO: 'Прогресс', TIP: 'Совет', MEAL_LOG: 'Питание', FREEFORM: 'Запись',
};

export default function PostCard({ post, currentUserId }) {
  const [liked, setLiked] = useState(false), [saved, setSaved] = useState(false);
  const [likes, setLikes] = useState(post.likesCount || 0);
  const initials = (post.authorDisplayName || 'U').split(/\s+/).map(x => x[0]).join('').slice(0, 2).toUpperCase();
  const action = type => FeedService.trackAction(post.id, currentUserId, type);
  const like = () => { setLiked(x => !x); setLikes(x => x + (liked ? -1 : 1)); action(liked ? 'VIEW' : 'LIKE'); };
  const save = () => { setSaved(x => !x); action(saved ? 'VIEW' : 'SAVE'); };
  const share = () => { action('SHARE'); navigator.clipboard?.writeText(`${window.location.origin}/posts/${post.id}`).catch(() => {}); };
  return <article className="bg-white rounded-2xl border border-gray-100 overflow-hidden shadow-sm">
    <div className="flex items-center justify-between px-5 pt-5"><div className="flex items-center gap-3"><span className="w-10 h-10 rounded-full bg-green-100 text-green-800 grid place-items-center font-bold">{initials}</span><div><p className="font-semibold text-gray-900 text-sm m-0">{post.authorDisplayName || 'Пользователь'}</p><p className="text-xs text-gray-500 m-0">{post.createdAt ? new Date(post.createdAt).toLocaleString('ru-RU') : ''}</p></div></div><span className="text-xs font-semibold px-2 py-1 rounded-full bg-green-50 text-green-800">{labels[post.postType] || 'Запись'}</span></div>
    {post.mediaUrls?.[0] && <img src={post.mediaUrls[0]} alt="Материал публикации" className="w-full max-h-80 object-cover mt-4" />}
    <div className="px-5 py-4">{post.text && <p className="text-gray-800 leading-relaxed">{post.text}</p>}<div className="flex gap-4 text-xs text-gray-500">{post.durationMinutes && <span className="flex gap-1"><Clock className="w-4" />{post.durationMinutes} мин</span>}{post.caloriesBurned && <span className="flex gap-1"><Flame className="w-4" />{post.caloriesBurned} ккал</span>}</div>{post.tags?.length > 0 && <div className="flex flex-wrap gap-2 mt-3">{post.tags.slice(0, 5).map(tag => <span className="text-xs text-green-700 font-medium" key={tag}>#{tag}</span>)}</div>}</div>
    <div className="flex justify-between px-5 py-3 border-t border-gray-100"><div className="flex gap-5"><button onClick={like} className={liked ? 'text-red-500 flex gap-1' : 'text-gray-500 flex gap-1'}><Heart className={`w-5 ${liked ? 'fill-current' : ''}`} />{likes}</button><button onClick={share} className="text-gray-500"><Share2 className="w-5" /></button></div><button onClick={save} className={saved ? 'text-yellow-500' : 'text-gray-400'}><Bookmark className={`w-5 ${saved ? 'fill-current' : ''}`} /></button></div>
  </article>;
}
