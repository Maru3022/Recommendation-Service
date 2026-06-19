# Recommendation Service

Personalized fitness post feed microservice — part of a Spring Boot 3.4.2 / Java 21 fitness app.

A production-ready recommendation engine that provides personalized content feeds using hybrid ranking algorithms combining collaborative filtering, content-based recommendations, social signals, and trending analysis.

## Features

### Feed API (`/api/v1/feed`)
| Endpoint | Description |
|---|---|
| `GET /api/v1/feed/personalized?userId=&page=&size=` | **Hybrid-ranked personal feed** — blends social, collaborative, content-based, trending, and freshness signals |
| `GET /api/v1/feed/following?userId=&page=&size=` | Chronological feed from followed users only |
| `GET /api/v1/feed/trending?page=&size=` | Global trending posts (72-hour interaction window) |
| `GET /api/v1/feed/collaborative?userId=&limit=` | Collaborative filtering recommendations using kNN |
| `GET /api/v1/feed/content-based?userId=&limit=` | Content-based recommendations using semantic search |
| `GET /api/v1/feed/social?userId=&limit=` | Posts from followed users |
| `POST /api/v1/feed/action` | Record user action (VIEW/LIKE/COMMENT/SHARE/SAVE) |
| `POST /api/v1/feed/invalidate?userId=` | Invalidate feed cache for user |

### Post API (`/api/v1/posts`)
| Endpoint | Description |
|---|---|
| `POST /api/v1/posts` | Create post |
| `GET /api/v1/posts/{postId}` | Get post by ID |
| `GET /api/v1/posts/author/{authorId}` | Get posts by author |
| `DELETE /api/v1/posts/{postId}` | Delete post (author-only) |
| `POST /api/v1/posts/{postId}/actions` | Track action — VIEW / LIKE / COMMENT / SHARE / SAVE / HIDE / REPORT |
| `GET /api/v1/posts/{userId}/history` | User action history |

### Social Graph API (`/api/v1/social`)
| Endpoint | Description |
|---|---|
| `POST /api/v1/social/{userId}/follow/{targetId}` | Follow a user |
| `DELETE /api/v1/social/{userId}/follow/{targetId}` | Unfollow |
| `GET /api/v1/social/{userId}/following` | Who userId follows |
| `GET /api/v1/social/{userId}/followers` | Who follows userId |

### Semantic Search API (`/api/v1/semantic`)
| Endpoint | Description |
|---|---|
| `POST /api/v1/semantic/search` | Semantic search using vector embeddings and AI explanations |

### Observability
- `GET /actuator/health`, `/actuator/metrics`, `/actuator/prometheus`
- SpringDoc OpenAPI UI at `http://localhost:8026/swagger-ui.html`
- **Grafana logs & metrics**: `docker compose up -d` → http://localhost:3000 (`admin` / `admin`) — see [docs/observability.md](docs/observability.md)

## Hybrid ranking algorithm

Final score = **0.35** × social + **0.25** × collaborative + **0.20** × content + **0.15** × trending + **0.05** × freshness

Weights are configurable in `application.properties` under `recommendation.feed.weights.*` — designed for A/B experimentation without rebuilding.

Additional guards:
- **Freshness decay**: exponential `exp(-λ·ageHours)`, half-life ≈ 48 h
- **Diversity guard**: max 2 posts per author per page
- **Negative signals**: posts from muted users excluded (`social:{userId}:muted` Redis set)

## Technology stack

| Layer | Tech |
|---|---|
| Runtime | Java 21, Spring Boot 3.4.2 |
| Web | Spring MVC (servlet-based, non-reactive) |
| Search | Spring Data Elasticsearch 5.x, kNN vector search |
| Cache | Spring Data Redis (user preferences, social graph, interest embeddings) |
| Messaging | Apache Kafka (`train.completed`, `training.created`) |
| AI | Spring AI 1.0.0-M6 — OpenAI `text-embedding-ada-002` + `gpt-4o-mini` |
| Service discovery | Spring Cloud Eureka client |
| Docs | SpringDoc OpenAPI (webmvc-ui) |
| Metrics | Micrometer + Prometheus |
| Frontend | React 18, Tailwind CSS, Axios |
| Infra | Docker (multi-stage, non-root), Kubernetes + Kustomize, GitHub Actions |

## Redis key schema

| Key | Type | Description |
|---|---|---|
| `user:{id}:fav_category` | String | Top category for quick personalization |
| `user:{id}:category_preferences` | Hash | `category → score` and `tag:{tag} → score` |
| `user:{id}:interest_embedding` | Binary | EMA float[1536] of last liked/saved post embeddings |
| `social:{id}:following` | Set | UserIds this user follows |
| `social:{id}:followers` | Set | UserIds that follow this user |
| `social:{id}:muted` | Set | Muted userIds (negative signal) |
| `feed:userId:page:N` | String | Cached feed results with 10-min TTL |
| `trending:posts` | ZSet | Trending posts with engagement scores |
| `similar_users:userId` | String | Cached similar users list |

## Elasticsearch indices

| Index | Document | Purpose |
|---|---|---|
| `posts` | `PostDoc` | Post content + 1536-dim embedding |
| `post_actions` | `PostAction` | Interaction log with timestamp (for trending window) |
| `user_profiles` | `UserProfileDoc` | User interest embedding for kNN similarity search |

## Kafka topics consumed

| Topic | Action |
|---|---|
| `train.completed` | Auto-generates a `WORKOUT_COMPLETED` post |
| `training.created` | Auto-generates a `TIP` post (if text is present) |

> **Extension point**: add `nutrition.logged` listener in `TrainingEventConsumer` to auto-generate `MEAL_LOG` posts from Training-Nutrition events.

## Running locally

```bash
# Backend (port 8026)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend (port 5000, proxies to :8026)
cd frontend && npm install && npm start
```

Requires: Elasticsearch, Redis, Kafka, PostgreSQL — start everything with Docker Compose:

```bash
docker compose up -d
```

See [docs/observability.md](docs/observability.md) for Grafana log viewing. For Kubernetes, use the `k8s/` overlays.

## Configuration

Key properties in `application.properties`:

```properties
recommendation.feed.weights.social=0.35
recommendation.feed.weights.collaborative=0.25
recommendation.feed.weights.content=0.20
recommendation.feed.weights.trending=0.15
recommendation.feed.weights.freshness=0.05

recommendation.feed.trending-window-hours=72
recommendation.feed.freshness-half-life-hours=48
recommendation.feed.knn-candidates-multiplier=10
recommendation.feed.knn-min-candidates=50

spring.ai.openai.api-key=${OPENAI_API_KEY:}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.embedding.options.model=text-embedding-ada-002
```

> **Note**: The social graph is intentionally stored in Redis for simplicity.
> At larger scale it should be extracted into a dedicated Social-Graph-Service (e.g. backed by Neo4j or PostgreSQL).

## Performance optimizations

- **Elasticsearch kNN**: User similarity search uses vector similarity instead of batch cosine similarity computation (20ms vs minutes)
- **Redis SCAN**: Cache invalidation uses non-blocking SCAN instead of KEYS command
- **Connection pooling**: HikariCP for PostgreSQL, Lettuce pool for Redis
- **Circuit breakers**: Resilience4j for Elasticsearch, Redis, and OpenAI API calls
- **Caching**: Feed results cached in Redis with 10-minute TTL
- **Batch operations**: JPA batch inserts/updates enabled
- **Shared scoring logic**: Consolidated duplicate scoring into ScoringService

## Testing

```bash
# Unit tests
./mvnw test

# Integration tests
./mvnw verify -P integration-tests

# Coverage report
./mvnw verify
# Report at target/site/jacoco/index.html
```

The project maintains >50% code coverage with JaCoCo enforcement.

## Recent improvements

### Critical fixes
1. **CF scaling**: Replaced batch-based cosine similarity with Elasticsearch kNN for user similarity search
2. **JaCoCo coverage**: Added comprehensive tests to meet 50% instruction coverage requirement
3. **Redis performance**: Replaced blocking KEYS command with non-blocking SCAN for cache invalidation
4. **Code quality**: Consolidated duplicate scoring logic into shared ScoringService

### API enhancements
- Added dedicated endpoints for different feed types (personalized, following, trending, collaborative, content-based, social)
- Improved validation with Jakarta Bean Validation
- Added comprehensive error handling
- Enhanced DTOs with all necessary fields (sharesCount, savesCount)

### Architecture improvements
- Created UserProfileDoc for Elasticsearch kNN user similarity
- Created UserProfileSearchRepository for vector similarity queries
- Unified scoring logic in ScoringService
- Enhanced repository methods for better query capabilities
