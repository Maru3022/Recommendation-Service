import React, { useState } from 'react';
import { User, ChevronDown } from 'lucide-react';

const UserSelector = ({ selectedUser, onUserChange }) => {
  const [isOpen, setIsOpen] = useState(false);
  
  const users = [
    { id: 'user1', name: 'John Doe', category: 'Electronics' },
    { id: 'user2', name: 'Jane Smith', category: 'Books' },
    { id: 'user3', name: 'Bob Johnson', category: 'Fashion' },
    { id: 'user4', name: 'Alice Brown', category: 'Home & Garden' },
    { id: 'user5', name: 'Charlie Wilson', category: 'Sports' },
  ];

  const selectedUserObj = users.find(u => u.id === selectedUser) || users[0];

  const handleUserSelect = (user) => {
    onUserChange(user.id);
    setIsOpen(false);
  };

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center space-x-3 bg-white border border-gray-300 rounded-lg px-4 py-2 hover:bg-gray-50 transition-colors"
      >
        <User className="w-5 h-5 text-gray-600" />
        <div className="text-left">
          <div className="text-sm font-medium text-gray-900">
            {selectedUserObj.name}
          </div>
          <div className="text-xs text-gray-500">
            {selectedUserObj.category}
          </div>
        </div>
        <ChevronDown className={`w-4 h-4 text-gray-600 transition-transform ${
          isOpen ? 'transform rotate-180' : ''
        }`} />
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-64 bg-white border border-gray-200 rounded-lg shadow-lg z-50">
          <div className="py-1">
            {users.map((user) => (
              <button
                key={user.id}
                onClick={() => handleUserSelect(user)}
                className={`w-full px-4 py-3 text-left hover:bg-gray-50 transition-colors ${
                  user.id === selectedUser ? 'bg-blue-50 border-l-4 border-blue-500' : ''
                }`}
              >
                <div className="flex items-center space-x-3">
                  <User className={`w-5 h-5 ${
                    user.id === selectedUser ? 'text-blue-600' : 'text-gray-400'
                  }`} />
                  <div>
                    <div className={`text-sm font-medium ${
                      user.id === selectedUser ? 'text-blue-900' : 'text-gray-900'
                    }`}>
                      {user.name}
                    </div>
                    <div className="text-xs text-gray-500">
                      {user.category}
                    </div>
                  </div>
                </div>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default UserSelector;
