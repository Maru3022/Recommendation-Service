import React, { useState } from 'react';
import { User, ChevronDown } from 'lucide-react';

const DEMO_USERS = [
  { id: 'user1', name: 'Alex Petrov',   favCategory: 'strength' },
  { id: 'user2', name: 'Maria Ivanova', favCategory: 'running'  },
  { id: 'user3', name: 'Dan Korolev',   favCategory: 'nutrition'},
  { id: 'user4', name: 'Olga Smirnova', favCategory: 'yoga'     },
  { id: 'user5', name: 'Maxim Orlov',   favCategory: 'cycling'  },
];

const UserSelector = ({ selectedUser, onUserChange }) => {
  const [isOpen, setIsOpen] = useState(false);

  const selected = DEMO_USERS.find(u => u.id === selectedUser) || DEMO_USERS[0];

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center space-x-3 bg-white border border-gray-300 rounded-lg px-4 py-2 hover:bg-gray-50 transition-colors"
        aria-haspopup="listbox"
        aria-expanded={isOpen}
      >
        <User className="w-5 h-5 text-gray-600" aria-hidden="true" />
        <div className="text-left">
          <div className="text-sm font-medium text-gray-900">{selected.name}</div>
          <div className="text-xs text-gray-500">{selected.favCategory}</div>
        </div>
        <ChevronDown
          className={`w-4 h-4 text-gray-600 transition-transform ${isOpen ? 'rotate-180' : ''}`}
          aria-hidden="true"
        />
      </button>

      {isOpen && (
        <ul
          role="listbox"
          className="absolute right-0 mt-2 w-64 bg-white border border-gray-200 rounded-lg shadow-lg z-50"
        >
          {DEMO_USERS.map(user => (
            <li
              key={user.id}
              role="option"
              aria-selected={user.id === selectedUser}
              onClick={() => { onUserChange(user.id); setIsOpen(false); }}
              className={`w-full px-4 py-3 flex items-center space-x-3 cursor-pointer hover:bg-gray-50 transition-colors ${
                user.id === selectedUser ? 'bg-indigo-50 border-l-4 border-indigo-500' : ''
              }`}
            >
              <User className={`w-5 h-5 ${user.id === selectedUser ? 'text-indigo-600' : 'text-gray-400'}`} aria-hidden="true" />
              <div>
                <div className={`text-sm font-medium ${user.id === selectedUser ? 'text-indigo-900' : 'text-gray-900'}`}>
                  {user.name}
                </div>
                <div className="text-xs text-gray-500">{user.favCategory}</div>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default UserSelector;
