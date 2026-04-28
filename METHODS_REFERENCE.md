# Detailed method reference — Recommendation Service

This file documents **every method** in the backend codebase (`src/main/java`) and how it behaves (inputs, outputs, side-effects, error modes).

> Note: Lombok-generated getters/setters are not enumerated one-by-one; they are implied by fields.

---

## `com.example.recommendationservice.RecommendationServiceApplication`

### `public static void main(String[] args)`
- **Does**: boots Spring context and embedded web server.
- **Inputs**: CLI args (Spring properties/profiles).
- **Outputs**: none.
- **Side-effects**: starts the app; initializes beans/autoconfig.

---

## `com.example.recommendationservice.config.ElasticsearchConfig`

No methods. Enables Spring Data Elasticsearch repositories in `com.example.recommendationservice.repository`.

---

## `com.example.recommendationservice.config.KafkaConfig`

No methods. Enables Kafka listener processing (`@KafkaListener`).

---

## `com.example.recommendationservice.config.RedisConfig`

### `public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory)`
- **Does**: creates a `RedisTemplate<String,Object>` with:
  - key/hashKey serializers = `StringRedisSerializer`
  - value/hashValue serializers = `GenericJackson2JsonRedisSerializer`
- **Inputs**: `RedisConnectionFactory` from Spring Boot autoconfig.
- **Outputs**: `RedisTemplate` bean.
- **Side-effects**: none besides bean registration.
- **Important**: config is conditional on `RedisConnectionFactory` presence; in test profile Redis autoconfig can be excluded safely.

---

## `com.example.recommendationservice.config.OpenApiConfig`

### `public OpenAPI recommendationServiceOpenAPI()`
- **Does**: constructs and registers OpenAPI metadata for Swagger UI.
- **Outputs**: `OpenAPI` bean.
- **Side-effects**: none.

---

## `com.example.recommendationservice.config.RequestIdFilter`

### `protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)`
- **Does**:
  - reads `X-Request-Id` header; if missing generates UUID
  - puts request id into MDC (`requestId`) for logging correlation
  - sets `X-Request-Id` response header
  - always clears MDC at the end
- **Side-effects**: adds response header; sets MDC for downstream logs.

---

## `com.example.recommendationservice.controller.RecommendationController`

Base path: `/api/recommendations`. Bean validation is enabled via `@Validated`.

### `public ResponseEntity<RecommendationResponse> getRecommendations(String userId, int page, int size)`
- **HTTP**: `GET /api/recommendations/{userId}?page=&size=`
- **Validation**:
  - `userId`: not blank, max length 50
  - `page`: `>= 0`
  - `size`: 1..100
- **Does**: delegates to `RecommendationService.getRecommendations`.
- **Returns**: 200 + `RecommendationResponse`.
- **Errors**:
  - validation errors -> 400 (handled globally)
  - Redis down -> falls back to non-personalized recommendations in service

### `public ResponseEntity<List<ProductDoc>> getPopular(int limit)`
- **HTTP**: `GET /api/recommendations/popular?limit=`
- **Validation**: `limit` 1..100
- **Does**: delegates to `RecommendationService.getPopularProducts`.
- **Returns**: 200 + array of `ProductDoc`.

### `public ResponseEntity<List<ProductDoc>> getCollaborativeRecommendations(String userId, int limit)`
- **HTTP**: `GET /api/recommendations/{userId}/collaborative?limit=`
- **Does**: delegates to `EnhancedRecommendationService.getCollaborativeRecommendations`.
- **Returns**: 200 + array of `ProductDoc`.

### `public ResponseEntity<List<ProductDoc>> getContentBasedRecommendations(String userId, int limit)`
- **HTTP**: `GET /api/recommendations/{userId}/content-based?limit=`
- **Does**: delegates to `EnhancedRecommendationService.getContentBasedRecommendations`.
- **Returns**: 200 + array of `ProductDoc`.

### `public ResponseEntity<List<ProductDoc>> getTrendingProducts(int limit)`
- **HTTP**: `GET /api/recommendations/trending?limit=`
- **Does**: delegates to `EnhancedRecommendationService.getTrendingProducts`.
- **Returns**: 200 + array of `ProductDoc`.

---

## `com.example.recommendationservice.controller.UserActionController`

Base path: `/api/user-actions`.

### `public ResponseEntity<Void> trackUserAction(UserAction userAction)`
- **HTTP**: `POST /api/user-actions`
- **Does**:
  - logs incoming action
  - delegates to `UserActionService.trackAction`
- **Returns**: 200 with empty body.
- **Side-effects**: persists `UserAction` in Elasticsearch; updates Redis preference signals.

### `public ResponseEntity<Iterable<UserAction>> getUserActionHistory(String userId)`
- **HTTP**: `GET /api/user-actions/{userId}/history`
- **Does**: delegates to `UserActionService.getUserActionHistory`.
- **Returns**: 200 + iterable of `UserAction`.

---

## `com.example.recommendationservice.controller.GlobalExceptionHandler`

### `public ResponseEntity<ErrorResponse> handleElasticsearchException(Exception ex)`
- **Triggers**: `NoSuchIndexException`
- **Returns**: 500 + safe message.

### `public ResponseEntity<ErrorResponse> handleRedisException(Exception ex)`
- **Triggers**: `RedisConnectionFailureException`
- **Returns**: 503 + safe message.

### `public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex)`
- **Triggers**: explicit argument checks in services/controllers
- **Returns**: 400 + original message.

### `public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex)`
- **Triggers**: bean validation failures (`@Min`, `@Max`, etc.)
- **Returns**: 400 + validation message (first violation).

### `public ResponseEntity<ErrorResponse> handleGenericException(Exception ex)`
- **Triggers**: fallback handler for unexpected exceptions.
- **Returns**: 500 + generic safe message.

---

## `com.example.recommendationservice.consumer.ProductSyncConsumer`

### `public void consumeProductUpdate(ProductDoc product)`
- **Kafka**: topic `product-updates`, group `rec-group`
- **Does**: delegates to `ProductSyncService.saveProduct` (best-effort).

---

## `com.example.recommendationservice.consumer.EventConsumer`

### `public void consumeUserAction(UserAction action)`
- **Kafka**: topic `user-actions`, group `rec-group`, concurrency 3
- **Does**: currently logs action type + userId (stub for future processing).

---

## `com.example.recommendationservice.service.RecommendationService`

### `public RecommendationResponse getRecommendations(String userId, int page, int size)`
- **Does**:
  - validates `page/size`
  - tries to read Redis `user:{userId}:fav_category`
  - if present: `findByCategory(category, pageable)`
  - else: `findAll(pageable)`
  - maps result to `RecommendationResponse`
- **Caching**: `@Cacheable("recommendations")` key includes `userId:page:size` (prevents collisions on size changes).
- **Errors**:
  - Redis failure is handled inside: returns non-personalized results.
  - ES errors bubble up to global handler.

### `public List<ProductDoc> getPopularProducts(int limit)`
- **Does**:
  - validates limit (1..100)
  - runs ES terms aggregation over `UserAction.productId`
  - fetches products via `findAllByIdIn(popularIds)`
- **Note**: order from repository may not match popularity order.

### `private void validatePagination(int page, int size)`
- **Does**: enforces `page >= 0`, `size` in 1..100.
- **Throws**: `IllegalArgumentException` (mapped to 400).

---

## `com.example.recommendationservice.service.ProductSyncService`

### `public void saveProduct(ProductDoc productDoc)`
- **Does**: indexes product into Elasticsearch (`productSearchRepository.save`).
- **Failure mode**: logs error and **does not throw** (best-effort).

---

## `com.example.recommendationservice.service.EnhancedRecommendationService`

### `public List<ProductDoc> getCollaborativeRecommendations(String userId, int limit)`
- **Does**:
  - finds similar users by overlap of product interactions
  - collects products viewed/liked by similar users
  - removes products already interacted by current user
  - returns up to `limit` products
- **Failure mode**: returns empty list on error (logs).

### `public List<ProductDoc> getContentBasedRecommendations(String userId, int limit)`
- **Does**:
  - reads Redis hash `user:{userId}:category_preferences`
  - selects up to 3 top categories by score
  - fetches products per category and filters out already-interacted
  - returns up to `limit`
- **Robustness**: null-safe if repository returns null page/content.

### `public List<ProductDoc> getTrendingProducts(int limit)`
- **Does**: iterates all actions, counts views/likes per productId, returns top product docs.
- **Note**: baseline implementation; for production prefer ES aggregations + time window.

### `private List<String> findSimilarUsers(String userId, int limit)`
- **Does**: counts other users interacting with the same productIds.

### `private Set<String> getUserInteractedProducts(String userId)`
- **Does**: returns set of productIds from `actionRepository.findByUserId`.

---

## `com.example.recommendationservice.service.UserActionService`

### `public void trackAction(UserAction userAction)`
- **Does**:
  - generates `id` if missing (UUID)
  - saves action to Elasticsearch
  - updates Redis preference signals (best-effort)
- **Throws**: wraps failures in `RuntimeException` (becomes 500 unless handled otherwise).

### `public Iterable<UserAction> getUserActionHistory(String userId)`
- **Does**: returns actions from Elasticsearch by userId.
- **Throws**: `RuntimeException` on errors.

### `private void updateUserPreferences(UserAction userAction)`
- **Does**:
  - if action is `view`/`like`: increments Redis hash `category_preferences` (currently uses `"general"` category)
  - updates `fav_category`
- **Failure mode**: logs warn; does not throw.

### `private void updateFavoriteCategory(String userId)`
- **Does**: reads preference hash, chooses max score key, writes `user:{userId}:fav_category`.

---

## Repositories

### `ProductSearchRepository`
- `findByCategory(String category, Pageable pageable)` — page by category
- `findAllByIdIn(List<String> ids)` — fetch by ids
- plus standard `ElasticsearchRepository` CRUD.

### `ActionRepository`
- `findByUserId(String userId)`
- `findByProductId(String productId)`
- plus standard `ElasticsearchRepository` CRUD.

