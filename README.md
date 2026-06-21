# 🤖 Recommendation Service

AI-сервис персонализированных рекомендаций фитнес-платформы [FitFlow](https://github.com/Maru3022/project-hub) — гибридная лента (collaborative + content-based + social + trending), социальный граф и семантический поиск на векторных embeddings с RAG-объяснением результатов.

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](.)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen?logo=spring)](.)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-OpenAI-412991?logo=openai)](.)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-kNN%20vector%20search-005571?logo=elasticsearch)](.)
[![Redis](https://img.shields.io/badge/Redis-Cache-DC382D?logo=redis)](.)
[![Observability](https://img.shields.io/badge/Observability-Prometheus%20%2B%20Grafana%20%2B%20Loki-F46800?logo=grafana)](.)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-OWASP%20%2B%20SBOM%20%2B%20Trivy-2088FF?logo=githubactions)](.)

---

## Что делает сервис

Не «ещё один список постов по дате создания», а полноценный recommendation-движок:

- **Гибридная персональная лента** — финальный скор поста собирается из четырёх независимых источников кандидатов с настраиваемыми весами.
- **Семантический поиск (RAG)** — свободный текстовый запрос превращается в вектор, ищется kNN-поиском в Elasticsearch, а GPT объясняет, почему результат релевантен.
- **Социальный граф** — подписки/подписчики, лента «только от тех, кого я читаю».
- **Трендовый раздел** — глобально популярные посты за скользящее окно, с фоновым пересчётом и кэшированием в Redis.
- **Учёт интересов в реальном времени** — действия пользователя (лайк/коммент/шер/сохранение) обновляют его embedding интересов через экспоненциальное скользящее среднее (EMA), без переобучения модели.

## Гибридный алгоритм ранжирования

```
score = 0.35 × social + 0.25 × collaborative + 0.20 × content + 0.15 × trending + 0.05 × freshness
```

| Источник | Как считается |
|---|---|
| **Social** | посты от тех, на кого подписан пользователь |
| **Collaborative** | kNN по похожим пользователям (схожесть профилей, кэш в Redis на 6 часов) |
| **Content-based** | косинусная близость embedding-вектора поста к вектору интересов пользователя |
| **Trending** | engagement-score (лайки/комменты/сохранения/шеры) за скользящее окно **72 часа** |
| **Freshness** | экспоненциальное затухание по возрасту поста, half-life ≈ **48 часов** |

Кандидаты собираются параллельно из всех источников, дедуплицируются, после чего применяется:
- **diversity guard** — не больше **2 постов одного автора** на страницу выдачи;
- **negative signals** — посты от заглушенных (`muted`) пользователей исключаются ещё на этапе кандидатов.

Все веса и пороги — не захардкожены, а вынесены в конфиг (`recommendation.feed.weights.*`, `recommendation.feed.trending-window-hours`, `recommendation.feed.freshness-half-life-hours`...) — ранжирование можно A/B-тестировать без пересборки приложения.

## AI / RAG семантический поиск

```
Текстовый запрос
      │
      ▼
EmbeddingService → OpenAI text-embedding-ada-002 (1536-dim вектор)
      │
      ▼
Elasticsearch kNN-поиск по полю posts.embedding
      │  (если kNN не дал результатов)
      ▼
Fallback: multi_match по text / tags / category
      │
      ▼
GPT-4o-mini объясняет, почему каждый пост релевантен запросу
```

Тот же `EmbeddingService` считает векторы и для интересов пользователя: при каждом значимом действии (like/save/share) embedding профиля обновляется через EMA (`recommendation.action.interest-embedding-alpha-percent`) — это и есть источник вектора для content-based кандидатов, без отдельного ML-пайплайна обучения.

## API

### Feed API (`/api/v1/feed`)

| Method | Endpoint | Описание |
|---|---|---|
| `GET` | `/feed/personalized?userId=&page=&size=` | Гибридная персональная лента (все 5 сигналов) |
| `GET` | `/feed/following?userId=&page=&size=` | Хронологическая лента только от подписок |
| `GET` | `/feed/trending?page=&size=` | Глобальные трендовые посты (окно 72 часа) |
| `GET` | `/feed/collaborative?userId=&limit=` | Только collaborative-filtering кандидаты |
| `GET` | `/feed/content-based?userId=&limit=` | Только content-based кандидаты (семантическая близость) |
| `GET` | `/feed/social?userId=&limit=` | Только посты от подписок |
| `POST` | `/feed/action` | Зафиксировать действие пользователя (VIEW/LIKE/COMMENT/SHARE/SAVE) |
| `POST` | `/feed/invalidate?userId=` | Сбросить кэш ленты пользователя |

### Post API (`/api/v1/posts`)

| Method | Endpoint | Описание |
|---|---|---|
| `POST` | `/posts` | Создать пост |
| `GET` | `/posts/{postId}` | Получить пост по id |
| `GET` | `/posts/author/{authorId}` | Посты конкретного автора |
| `DELETE` | `/posts/{postId}` | Удалить пост (только автор) |
| `POST` | `/posts/{postId}/actions` | Зафиксировать действие (VIEW/LIKE/COMMENT/SHARE/SAVE/HIDE/REPORT) |
| `GET` | `/posts/{userId}/history` | История действий пользователя |

### Social Graph API (`/api/v1/social`)

| Method | Endpoint | Описание |
|---|---|---|
| `POST` | `/social/{userId}/follow/{targetId}` | Подписаться |
| `DELETE` | `/social/{userId}/follow/{targetId}` | Отписаться |
| `GET` | `/social/{userId}/following` | На кого подписан пользователь |
| `GET` | `/social/{userId}/followers` | Кто подписан на пользователя |

### Semantic Search API (`/api/v1/semantic`)

| Method | Endpoint | Описание |
|---|---|---|
| `POST` | `/semantic/search` | Семантический поиск по векторным embeddings с AI-объяснением релевантности |

### Технические эндпоинты

| Endpoint | Описание |
|---|---|
| `/swagger-ui.html` | OpenAPI / Swagger UI |
| `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` | Health-check и метрики |

## Архитектура и интеграция с платформой

```
                              ┌──────────────────┐
                              │   API Gateway      │
                              └────────┬──────────┘
                                       │ /api/recommendations/**
                                       ▼
                          ┌─────────────────────────┐       register      ┌────────────┐
                          │  Recommendation Service   ├─────────────────────►   Eureka    │
                          └────────────┬────────────┘                     └────────────┘
                                       │
          ┌─────────────┬──────────────┼───────────────┬────────────────┐
          ▼             ▼              ▼               ▼                ▼
     PostgreSQL      Redis          Elasticsearch   Apache Kafka     OpenAI API
   (posts, actions  (similar_users, (posts + user   training.created (embeddings,
    social graph)    trending,       embeddings,    → TrainingEventConsumer  GPT-4o-mini)
                      feed cache)    kNN-индекс)
```

Сервис слушает доменные события других сервисов платформы (`TrainingEventConsumer` на топик `training.created`) — например, тренировки пользователя могут влиять на контентные рекомендации без прямого синхронного вызова Training Service.

## Observability

Самый насыщенный observability-стек в платформе — поднимается отдельным compose-файлом:

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml up -d
```

| Компонент | Назначение |
|---|---|
| **Prometheus** | сбор метрик (`/actuator/prometheus`) + готовые `alerts.yml` для Alertmanager |
| **Grafana** | дашборды `recommendation-overview` (метрики) и `recommendation-logs` (логи), `admin`/`admin` на `localhost:3000` |
| **Loki + Promtail** | централизованный сбор и индексация логов контейнеров |
| **Correlation-id** | сквозной `CorrelationIdFilter` — один запрос можно протрассировать от лога до лога через все сервисы |

## DevSecOps в CI/CD

Pipeline на GitHub Actions (~460 строк) — самый требовательный в платформе:

| Этап | Что делает |
|---|---|
| **Tests** | юнит- и интеграционные тесты на нескольких версиях JDK, с покрытием |
| **OWASP Dependency-Check** | аудит уязвимостей Maven-зависимостей, с кэшированием базы CVE |
| **SBOM (CycloneDX)** | генерация Software Bill of Materials для образа |
| **Kubernetes Validate** | рендер Kustomize-оверлеев (staging/production) + валидация через `kubeconform` **до** деплоя |
| **Docker Build & Push** | multi-stage сборка, скан образа, публикация в GHCR |
| **Deploy to Staging** | автоматический деплой по прохождении всех проверок |

## Технологический стек

| Категория | Технологии |
|---|---|
| Язык / Framework | Java 21, Spring Boot 3.4.2 |
| AI / ML | Spring AI, OpenAI Embeddings (`text-embedding-ada-002`, 1536-dim), GPT-4o-mini |
| Поиск | Elasticsearch (kNN vector search + full-text fallback) |
| Данные | Spring Data JPA, PostgreSQL, HikariCP |
| Кэш | Redis (похожие пользователи, trending, кэш ленты) |
| Messaging | Spring Kafka (consumer доменных событий платформы) |
| Отказоустойчивость | Resilience4j (circuit breaker) |
| Service Discovery | Netflix Eureka Client |
| Observability | Prometheus, Grafana, Loki, Promtail, structured JSON logging (Logstash encoder) |
| Тестирование | JUnit 5, Mockito, integration-тесты (20 тестовых файлов) |
| Безопасность | OWASP Dependency-Check, CycloneDX SBOM |
| Контейнеризация / Deploy | Docker, Kubernetes + Kustomize (`base` / `overlays/staging` / `overlays/production`), HPA, PodDisruptionBudget |
| Frontend (демо) | React + Tailwind CSS — лента, выбор пользователя, карточки постов |

## Локальный запуск

### Вариант 1 — всё через Docker Compose (рекомендуется)

```bash
git clone https://github.com/Maru3022/Recommendation-Service.git
cd Recommendation-Service
cp .env.example .env        # при желании укажите OPENAI_API_KEY для AI-функций
docker compose up -d
```

Поднимутся PostgreSQL, Redis, Elasticsearch, Kafka и сам сервис. Проверка готовности:

```bash
docker compose ps
curl http://localhost:8026/actuator/health
```

С observability-стеком:

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml up -d
```

### Вариант 2 — локальная сборка

```bash
./mvnw clean package -DskipTests
java -jar target/recommendation-service-*.jar
```

| Что | Где |
|---|---|
| API | `http://localhost:8026` |
| Swagger UI | `http://localhost:8026/swagger-ui.html` |
| Grafana | `http://localhost:3000` (`admin`/`admin`) |
| Frontend-демо | `cd frontend && npm install && npm start` |

## Связанные репозитории

Часть микросервисной платформы [FitFlow](https://github.com/Maru3022/project-hub):

- [API Gateway](https://github.com/Maru3022/API_Gateway)
- [Eureka Server](https://github.com/Maru3022/Eureka-server)
- [Saga Orchestrator](https://github.com/Maru3022/Saga-Orchestrator)
- [Training Service](https://github.com/Maru3022/Training-Servive)
- [Trains Service](https://github.com/Maru3022/Trains-Service)
- [Nutrition Service](https://github.com/Maru3022/Training-Nutrition)
- [Notification Service](https://github.com/Maru3022/Training_Notification)
