# Recommendation Service Frontend

A modern React.js frontend for the Recommendation Service microservice, featuring personalized product recommendations, popular products, and user interaction tracking.

## Features

- 🎯 **Personalized Recommendations**: Get product suggestions based on user preferences
- 🔥 **Popular Products**: Browse trending and popular items
- 👥 **User Profiles**: Switch between different user personas
- 💝 **Interactive UI**: Like products, add to cart, and track interactions
- 📱 **Responsive Design**: Works seamlessly on desktop and mobile
- ⚡ **Real-time Updates**: Instant feedback and loading states
- 🎨 **Modern UI**: Beautiful design with Tailwind CSS

## Technology Stack

- **React 18** - Modern React with hooks
- **Tailwind CSS** - Utility-first CSS framework
- **Lucide React** - Beautiful icons
- **Axios** - HTTP client for API calls
- **React Router** - Client-side routing

## Prerequisites

- Node.js 16+ and npm
- Backend recommendation service running on `http://localhost:8026`

## Quick Start

1. **Install dependencies**:
   ```bash
   cd frontend
   npm install
   ```

2. **Start the development server**:
   ```bash
   npm start
   ```

3. **Open your browser**:
   Navigate to `http://localhost:3000`

## Available Scripts

- `npm start` - Runs the app in development mode
- `npm test` - Launches the test runner
- `npm run build` - Builds the app for production
- `npm run eject` - Ejects from Create React App (one-way operation)

## Configuration

### Environment Variables

Create a `.env` file in the frontend root:

```env
REACT_APP_API_URL=http://localhost:8026
```

### API Endpoints Used

- `GET /api/recommendations/{userId}` - Personalized recommendations
- `GET /api/recommendations/popular` - Popular products
- `GET /api/recommendations/{userId}/collaborative` - Collaborative filtering
- `GET /api/recommendations/{userId}/content-based` - Content-based filtering
- `GET /api/recommendations/trending` - Trending products
- `POST /api/user-actions` - Track user interactions

## User Personas

The frontend includes 5 predefined user personas:

1. **John Doe** - Electronics enthusiast
2. **Jane Smith** - Book lover
3. **Bob Johnson** - Fashion follower
4. **Alice Brown** - Home & Garden
5. **Charlie Wilson** - Sports fan

Each user has different preferences that affect the recommendations they receive.

## Features in Detail

### Personalized Recommendations
- Category-based personalization
- Pagination support
- Loading states and error handling

### Product Cards
- Product images with fallback
- Category badges
- Price display with currency formatting
- Star ratings
- Like/Add to cart actions
- Hover effects and animations

### User Interaction
- Switch between user personas
- Track likes and cart additions
- Real-time UI updates

### Responsive Design
- Mobile-first approach
- Adaptive grid layouts
- Touch-friendly interactions

## API Integration

The frontend automatically tracks user interactions:

```javascript
// Example: Track a product view
RecommendationService.trackUserAction('user1', 'product123', 'view');

// Example: Track a like
RecommendationService.trackUserAction('user1', 'product123', 'like');
```

## Error Handling

- Network error handling with user-friendly messages
- Fallback product images
- Graceful degradation when API is unavailable
- Loading spinners for better UX

## Performance Optimizations

- Component lazy loading
- Image optimization
- Debounced API calls
- Efficient state management

## Development

### Component Structure

```
src/
├── components/          # Reusable UI components
│   ├── ProductCard.js   # Product display card
│   ├── UserSelector.js  # User persona selector
│   └── LoadingSpinner.js # Loading indicator
├── services/           # API service layer
│   └── RecommendationService.js
├── App.js             # Main application component
├── index.js           # Application entry point
└── index.css          # Global styles
```

### Adding New Features

1. Create new components in `src/components/`
2. Add API methods to `RecommendationService.js`
3. Update `App.js` to integrate new features
4. Add responsive styles using Tailwind CSS

## Build and Deployment

### Production Build

```bash
npm run build
```

This creates an optimized production build in the `build/` folder.

### Docker Deployment

```dockerfile
# Multi-stage build for production
FROM node:16-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build

FROM nginx:stable-alpine
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

## Troubleshooting

### Common Issues

1. **API Connection Errors**:
   - Ensure backend is running on port 8026
   - Check CORS configuration in backend
   - Verify API URL in `.env` file

2. **Missing Dependencies**:
   ```bash
   rm -rf node_modules package-lock.json
   npm install
   ```

3. **Build Failures**:
   - Check Node.js version (16+ required)
   - Clear npm cache: `npm cache clean --force`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new features
5. Submit a pull request

## License

This project is licensed under the MIT License.
