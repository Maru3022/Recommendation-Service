# Recommendation Service

A comprehensive recommendation microservice built with Spring Boot that provides personalized and popular product recommendations. The service uses PostgreSQL, Elasticsearch, Redis, and Kafka, and is fully containerized for both Docker and Kubernetes deployments.

## 🚀 Features

### Core Functionality
- **Personalized Recommendations**: Category-based personalization using Redis user preferences
- **Popular Products**: Most viewed/liked products across all users
- **Collaborative Filtering**: Recommendations based on similar users' behavior
- **Content-Based Filtering**: Recommendations based on user's category preferences
- **Trending Products**: Currently popular items based on recent activity
- **User Action Tracking**: Track views, likes, and cart additions

### Technical Features
- **Real-time Updates**: Kafka-based product synchronization
- **High Performance**: Redis caching and Elasticsearch for fast search
- **Scalable Architecture**: Microservice design with horizontal scaling support
- **Comprehensive Monitoring**: Prometheus metrics and health endpoints
- **API Documentation**: OpenAPI/Swagger documentation
- **Modern Frontend**: React.js interface with Tailwind CSS

## 🛠 Technology Stack

### Backend
- **Java 21** with Spring Boot 3.4.2
- **Spring Data JPA** with PostgreSQL
- **Spring Data Elasticsearch** for product search
- **Spring Data Redis** for caching and user preferences
- **Spring Kafka** for real-time product updates
- **Spring Boot Validation** for input validation
- **SpringDoc OpenAPI** for API documentation
- **Micrometer Prometheus** for metrics

### Frontend
- **React 18** with modern hooks
- **Tailwind CSS** for styling
- **Axios** for API communication
- **Lucide React** for icons

### Infrastructure
- **Docker** with multi-stage builds
- **Kubernetes** with Kustomize overlays
- **GitHub Actions** for CI/CD
- **H2** for testing

## 📡 API Documentation

### Base URL
- Development: `http://localhost:8026`
- Production: `https://api.recommendation-service.com`

### Endpoints

#### Recommendations
- `GET /api/recommendations/{userId}` - Get personalized recommendations
- `GET /api/recommendations/popular` - Get popular products
- `GET /api/recommendations/{userId}/collaborative` - Collaborative filtering recommendations
- `GET /api/recommendations/{userId}/content-based` - Content-based recommendations
- `GET /api/recommendations/trending` - Get trending products

#### User Actions
- `POST /api/user-actions` - Track user interaction
- `GET /api/user-actions/{userId}/history` - Get user action history

#### Documentation
- `GET /swagger-ui.html` - Interactive API documentation
- `GET /v3/api-docs` - OpenAPI specification

### Example Requests

```bash
# Get personalized recommendations
curl "http://localhost:8026/api/recommendations/user1?page=0&size=10"

# Get popular products
curl "http://localhost:8026/api/recommendations/popular?limit=20"

# Track user action
curl -X POST "http://localhost:8026/api/user-actions" \
  -H "Content-Type: application/json" \
  -d '{"userId":"user1","productId":"123","actionType":"view"}'
```

## 🚀 Quick Start

### Prerequisites
- **Java 21+** and Maven
- **Node.js 16+** and npm (for frontend)
- **Docker** and **Docker Compose** (for infrastructure)

### 1. Start Infrastructure Services
```bash
docker-compose -f docker-compose.dev.yml up -d
```

### 2. Start Backend Service
```bash
./mvnw spring-boot:run
```

### 3. Start Frontend (New Terminal)
```bash
cd frontend
npm install
npm start
```

### 4. Access the Application
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8026
- **API Documentation**: http://localhost:8026/swagger-ui.html
- **Kafka UI**: http://localhost:8080 (optional)

## 🧪 Testing

### Run Backend Tests
```bash
./mvnw test
```

### Run Frontend Tests
```bash
cd frontend
npm test
```

### Test Coverage
- Unit tests for all service layers
- Integration tests for API endpoints
- Controller tests with MockMvc
- Frontend component tests

## 📦 Deployment

### Docker Build
```bash
docker build -t recommendation-service:latest .
```

### Kubernetes Deployment
```bash
# Deploy to staging
kubectl apply -k k8s/overlays/staging

# Deploy to production
kubectl apply -k k8s/overlays/production
```

### Environment Variables
- `SPRING_DATASOURCE_URL` - PostgreSQL connection
- `SPRING_ELASTICSEARCH_URIS` - Elasticsearch nodes
- `SPRING_DATA_REDIS_HOST` - Redis server
- `SPRING_KAFKA_BOOTSTRAP_SERVERS` - Kafka brokers
- `EUREKA_SERVER_URL` - Service discovery

## 📊 Monitoring

### Health Endpoints
- `/actuator/health` - Overall health
- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe

### Metrics
- `/actuator/prometheus` - Prometheus metrics
- `/actuator/metrics` - Spring Boot metrics

### Key Metrics
- Request duration and count
- Recommendation generation time
- Cache hit rates
- Database connection pool status

## 🔧 Configuration

### Profiles
- `dev` - Development with H2 database
- `default` - Production-ready configuration
- `test` - Testing configuration

### Caching Strategy
- **Redis**: User preferences and session data
- **Application Cache**: Popular products (TTL: 5 minutes)
- **Elasticsearch**: Product search and aggregations

### Recommendation Algorithms
1. **Category-based**: User's favorite categories from Redis
2. **Collaborative**: Similar users' behavior patterns
3. **Content-based**: Category preference matching
4. **Trending**: Recent activity analysis

## 🛡 Security

### Implemented Measures
- **CORS**: Configured for specific origins
- **Input Validation**: Bean validation on all inputs
- **Rate Limiting**: Configurable request limits
- **Error Handling**: Sanitized error responses

### Best Practices
- No sensitive data in logs
- Secure defaults for all configurations
- Regular dependency updates
- Security scanning in CI/CD

## Локальный запуск

Требования:

- JDK 21+
- Maven или Maven Wrapper
- PostgreSQL
- Elasticsearch
- Redis
- Kafka

Команды:

```bash
./mvnw clean test
./mvnw spring-boot:run
```

Сервис будет доступен по адресу:

```text
http://localhost:8026
```

## Конфигурация через переменные окружения

Основные runtime-параметры вынесены во внешние env vars:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_ELASTICSEARCH_URIS`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `SPRING_KAFKA_CONSUMER_GROUP_ID`
- `EUREKA_SERVER_URL`

Это позволяет без перепаковки образа использовать разные конфигурации для staging и production.

## Docker

Контейнер собирается через multi-stage Dockerfile:

- build stage использует Maven + Temurin 21
- runtime stage использует Temurin 21 JRE Alpine
- `mvnw` получает executable permission в контейнере
- приложение запускается от non-root пользователя `spring`

Сборка образа:

```bash
docker build -t recommendation-service:local .
```

## Kubernetes

В репозитории добавлена структура `k8s/`:

```text
k8s/
  base/
    deployment.yaml
    service.yaml
    hpa.yaml
    pdb.yaml
    kustomization.yaml
    secret.example.yaml
  overlays/
    staging/
    production/
```

### Что уже предусмотрено

- `Deployment` с rolling update стратегией
- `Service`
- `HorizontalPodAutoscaler`
- `PodDisruptionBudget`
- `Prometheus` annotations
- `startupProbe`, `readinessProbe`, `livenessProbe`
- отдельные overlays для `staging` и `production`
- разные namespace для окружений
- ingress для каждого окружения
- внешний secret для чувствительных данных

### Подготовка секретов

В репозиторий добавлен шаблон:

```text
k8s/base/secret.example.yaml
```

Перед деплоем создай реальный secret на основе этого шаблона и не коммить его в репозиторий.

Для pull из GHCR кластеру также понадобится `imagePullSecret` с именем `ghcr-pull-secret`.

### Рендеринг manifests

```bash
kubectl kustomize k8s/overlays/staging
kubectl kustomize k8s/overlays/production
```

### Деплой вручную

```bash
kubectl apply -k k8s/overlays/staging
kubectl apply -k k8s/overlays/production
```

## CI/CD

Workflow находится в:

```text
.github/workflows/main.yml
```

### Что делает pipeline

1. Гоняет тесты на Java 21.
2. Выполняет `clean verify` на основной версии Java 21.
3. Рендерит Kubernetes overlays и валидирует их через `kubectl apply --dry-run=client`.
4. Собирает Docker image.
5. На `main` пушит image в GHCR с тегами:
   - `latest`
   - `${github.sha}`
6. Деплоит staging через `kubectl apply -k`.
7. После staging обновляет образ в deployment и ждет rollout.
8. Затем тем же способом деплоит production.

### Какие secrets нужны в GitHub Actions

- `KUBE_CONFIG_STAGING` — base64 kubeconfig для staging-кластера
- `KUBE_CONFIG_PRODUCTION` — base64 kubeconfig для production-кластера

`GITHUB_TOKEN` используется для публикации образа в GHCR автоматически.
Если `KUBE_CONFIG_STAGING` или `KUBE_CONFIG_PRODUCTION` не заданы, соответствующий deploy job будет автоматически пропущен, а build и publish этапы продолжат работать.

## Health endpoints для Kubernetes

Используются стандартные Spring Boot Actuator endpoints:

- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/prometheus`

## Проверка проекта

Основная локальная проверка:

```bash
./mvnw -B -ntp clean test
./mvnw -B -ntp clean verify -DskipTests
```

## Что важно помнить

- Kubernetes manifests предполагают, что PostgreSQL, Kafka, Redis и Elasticsearch уже доступны в соответствующем окружении.
- Хосты ingress в overlays заданы как шаблонные:
  - `recommendation-staging.example.com`
  - `recommendation.example.com`
  Их нужно заменить на реальные домены.
- Если планируется реальный production rollout, стоит добавить sealed secrets или внешний secret manager.
