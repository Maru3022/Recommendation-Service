## Recommendation Service

Recommendation Service is a high‑performance Spring Boot microservice that returns personalized product recommendations, backed by PostgreSQL, Elasticsearch, Redis, Kafka and k6 load tests.  
The project is fully containerized with Docker Compose and has a ready‑to‑use CI/CD pipeline on GitHub Actions.

---

### Features

- **REST API for recommendations**  
  - `GET /api/recommendations/{userId}?page={page}&size={size}`  
  - Returns a `RecommendationResponse` with:
    - list of products
    - current page
    - total elements
    - flag `hasNext`

- **Personalization via Redis**  
  - Reads favorite category from Redis key `user:{userId}:fav_category`.  
  - If category is present → queries only products of this category in Elasticsearch.  
  - If category is missing → falls back to “all products” search.

- **Search & storage in Elasticsearch**  
  - `ProductSearchRepository` for product search.  
  - `ActionRepository` for tracking user actions (clicks, views, etc.).  
  - Indices are created automatically via Spring Data Elasticsearch.

- **Product sync from Kafka**  
  - `ProductSyncConsumer` listens to topic `product-updates` (group `rec-group`).  
  - Every incoming `ProductDoc` is saved via `ProductSyncService` into Elasticsearch.

- **Resilience & error handling**  
  - `GlobalExceptionHandler`:
    - Elasticsearch “index not found” → HTTP `500`.
    - Redis connection issues → HTTP `503` with message _"Personalization data source error"_.  
    - Generic errors → HTTP `500` with a safe generic message.

- **Load testing with k6**  
  - `k6/load-test.js` aggressively hits `GET /api/recommendations/user_{id}` with stages:
    - `30s` → `50` VUs  
    - `1m` → `100` VUs  
    - `30s` ramp‑down  
  - Automatically runs inside Docker as the `k6` service.

- **CI/CD ready**  
  - GitHub Actions pipeline:
    - builds the JAR;
    - runs `mvn test` with H2 in‑memory DB for tests;
    - starts full Docker stack and executes k6 scenario.

---

### Tech Stack

- **Backend**: Java 17, Spring Boot 3 (Web, Data JPA, Data Redis, Data Elasticsearch, Spring Kafka)  
- **Datastores**: PostgreSQL, Elasticsearch, Redis  
- **Messaging**: Kafka + Zookeeper  
- **Load testing**: k6  
- **Infrastructure**: Docker, Docker Compose, GitHub Actions

---

## Quick Start

### 1. Run everything with Docker (recommended)

Requirements:
- Docker
- Docker Compose v2 (`docker compose` command)

From the project root:

```bash
docker compose up -d --build
```

This will start:
- `postgres` – main relational DB (`recommendation_db`)  
- `elasticsearch` – search engine  
- `redis` – personalization cache  
- `zookeeper` + `kafka` – message broker for product updates  
- `recommendation-service` – Spring Boot application on port `8026`  
- `k6` – container that automatically runs the load‑test script `k6/load-test.js`

Check that the API is up:

```bash
curl "http://localhost:8026/api/recommendations/user_1?page=0&size=10"
```

To view k6 logs:

```bash
docker compose logs k6 --tail=100
```

To stop everything:

```bash
docker compose down
```

---

### 2. Run locally without Docker (for development)

Requirements:
- JDK 17+
- Maven
- Running instances of PostgreSQL, Elasticsearch, Redis, Kafka (or adjust `application.properties`)

Steps:

```bash
mvn clean package
mvn spring-boot:run
```

The service will be available at:

```text
http://localhost:8026
```

Main endpoint:

```text
GET /api/recommendations/{userId}?page=0&size=10
```

Example:

```bash
curl "http://localhost:8026/api/recommendations/user_123?page=0&size=10"
```

---

### 3. Running tests

- **Unit & integration tests** (with in‑memory H2 DB):

```bash
mvn test
```

The test profile:
- uses H2 instead of PostgreSQL;  
- mocks Elasticsearch repositories and disables Kafka listeners;  
- keeps tests fast and independent from external services.

---

## CI/CD (GitHub Actions)

The workflow is defined in `.github/workflows/main.yml` and does:

1. **Checkout & JDK**  
   - `actions/checkout@v4`  
   - `actions/setup-java@v4` with Java 17

2. **Build**  
   - `mvn clean package -DskipTests`

3. **Infrastructure & tests**  
   - `docker compose up -d --build` to start the whole stack.  
   - Wait for containers to boot.  
   - Run k6 scenario inside the `k6` container.

4. **Result check**  
   - Inspect k6 container exit code; if it is non‑zero, the pipeline fails.

This makes the repository immediately usable as a demo of a production‑like recommendation service with full infra and tests.

