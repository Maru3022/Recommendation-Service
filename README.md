# Recommendation Service

Personalized fitness post feed microservice — part of a Spring Boot 3.4.2 / Java 21 fitness app.

Replaces the previous "product recommendations" prototype with a fully reworked social feed engine modelled after Strava/Instagram but focused on workout posts, achievements, and training tips.

## Features

### Feed API (`/api/feed`)
| Endpoint | Description |
|---|---|
| `GET /api/feed/{userId}?page=&size=` | **Hybrid-ranked personal feed** — blends social, collaborative, content-based, trending, and freshness signals |
| `GET /api/feed/{userId}/following?page=&size=` | Chronological feed from followed users only |
| `GET /api/feed/trending?limit=` | Global trending posts (72-hour interaction window) |
| `GET /api/feed/{userId}/collaborative?limit=` | Raw collaborative-filter signal (debug/A-B) |
| `GET /api/feed/{userId}/content-based?limit=` | Raw content-based signal (debug/A-B) |
| `POST /api/feed/search` | Semantic (kNN + LLM) post search |

### Post API (`/api/posts`)
| Endpoint | Description |
|---|---|
| `POST /api/posts` | Create post |
| `GET /api/posts/{postId}` | Get post by ID |
| `DELETE /api/posts/{postId}?requestingUserId=` | Delete post (author-only) |
| `POST /api/posts/{postId}/actions` | Track action — VIEW / LIKE / COMMENT / SHARE / SAVE / HIDE / REPORT |
| `GET /api/posts/{userId}/history` | User action history |

### Social Graph API (`/api/social`)
| Endpoint | Description |
|---|---|
| `POST /api/social/{userId}/follow/{targetId}` | Follow a user |
| `DELETE /api/social/{userId}/follow/{targetId}` | Unfollow |
| `GET /api/social/{userId}/following` | Who userId follows |
| `GET /api/social/{userId}/followers` | Who follows userId |

### Observability
- `GET /actuator/health`, `/actuator/metrics`, `/actuator/prometheus`
- SpringDoc OpenAPI UI at `http://localhost:8026/swagger-ui.html`

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

## Elasticsearch indices

| Index | Document | Purpose |
|---|---|---|
| `posts` | `PostDoc` | Post content + 1536-dim embedding |
| `post_actions` | `PostAction` | Interaction log with timestamp (for trending window) |

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

Requires: Elasticsearch, Redis, Kafka, PostgreSQL (see `docker-compose` or k8s overlays).

## Configuration

Key properties in `application.properties`:

```properties
recommendation.feed.weights.social=0.35
recommendation.feed.weights.collaborative=0.25
recommendation.feed.weights.content=0.20
recommendation.feed.weights.trending=0.15
recommendation.feed.weights.freshness=0.05

spring.ai.openai.api-key=${OPENAI_API_KEY:}
spring.ai.openai.chat.options.model=gpt-4o-mini
spring.ai.openai.embedding.options.model=text-embedding-ada-002
```

> **Note**: The social graph is intentionally stored in Redis for simplicity.
> At larger scale it should be extracted into a dedicated Social-Graph-Service (e.g. backed by Neo4j or PostgreSQL).
