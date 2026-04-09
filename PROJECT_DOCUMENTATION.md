# Recommendation Service — полная документация проекта

## 1) Назначение и идея сервиса

`Recommendation-Service` — микросервис на Spring Boot, который отдает рекомендации товаров по пользователю.

В текущей реализации сервис:
- принимает REST-запросы на получение рекомендаций;
- использует Redis как источник персонализирующего признака (любимая категория пользователя);
- берет товары из Elasticsearch;
- принимает обновления каталога товаров из Kafka и синхронизирует их в Elasticsearch;
- имеет endpoint популярных товаров на основе агрегации действий пользователей;
- запускается локально и в Docker Compose-окружении;
- покрыт базовыми unit/web/context тестами;
- имеет CI pipeline с прогоном контейнеров и k6 нагрузки.

---

## 2) Технологический стек

- Java 17 (в `pom.xml`), Spring Boot 3.4.2
- Spring Web (REST API)
- Spring Data Elasticsearch (документы и репозитории)
- Spring Data Redis (персонализация через Redis ключ)
- Spring Kafka (consumers)
- Spring Data JPA + PostgreSQL (подключены как зависимости, но в бизнес-логике сейчас почти не задействованы)
- Spring Boot Actuator (`/actuator/health`, `/actuator/info`, `/actuator/metrics`)
- Lombok
- Maven
- Docker + Docker Compose
- k6 (нагрузочные тесты)
- GitHub Actions (CI/CD workflow)
- H2 (только для тестового окружения)

---

## 3) Структура проекта

Основные каталоги:

- `src/main/java/com/example/recommendationservice`
  - `config` — конфигурации Elasticsearch/Kafka/Redis
  - `consumer` — Kafka consumers
  - `controller` — REST endpoints + global exception handler
  - `model` — модели документов/DTO ошибок/ответов
  - `repository` — Elasticsearch репозитории
  - `service` — бизнес-логика рекомендаций и синхронизации товаров
- `src/main/resources`
  - `application.properties` — основной runtime-конфиг
  - `application.yml` — доп. конфиг для Eureka
  - `static/index.html` — простая UI-страница для ручной проверки
- `src/test/java/com/example/recommendationservice`
  - тесты сервисов/контроллеров/конфигурации контекста
- `src/test/resources/application-test.properties`
  - тестовый профиль (H2, отключение Eureka/Kafka listeners)
- `.github/workflows/main.yml`
  - CI pipeline
- `compose.yaml`
  - локальная инфраструктура + app + k6
- `k6/load-test.js`
  - сценарий нагрузочного тестирования
- `Dockerfile`
  - multi-stage сборка и запуск приложения

---

## 4) Конфигурация и параметры

### 4.1 Runtime (`application.properties`)

Ключевые параметры:

- `server.port=8026`
- PostgreSQL:
  - `spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://postgres:5432/recommendation_db}`
  - `spring.datasource.username=user`
  - `spring.datasource.password=secret`
- Elasticsearch:
  - `spring.elasticsearch.uris=${ELASTICSEARCH_URL:http://elasticsearch:9200}`
- Redis:
  - `spring.data.redis.host=${SPRING_DATA_REDIS_HOST:redis}`
  - `spring.data.redis.port=${SPRING_DATA_REDIS_PORT:6379}`
- Kafka:
  - `spring.kafka.bootstrap-servers=${SPRING_KAFKA_BOOTSTRAP_SERVERS:kafka:9092}`
  - `spring.kafka.consumer.group-id=rec-group`
- Actuator:
  - `management.endpoints.web.exposure.include=health,info,metrics`
  - `management.endpoint.health.show-details=always`

Нагрузочные настройки:
- `server.tomcat.threads.max=1000`
- `server.tomcat.max-connections=20000`
- `spring.datasource.hikari.maximum-pool-size=20`

### 4.2 Доп. YAML (`application.yml`)

Содержит настройки Eureka:
- `eureka.client.register-with-eureka=true`
- `eureka.client.fetch-registry=true`
- `eureka.client.service-url.defaultZone=${EUREKA_SERVER_URL:http://eureka-server:8761/eureka/}`
- `eureka.instance.prefer-ip-address=true`

### 4.3 Тестовый профиль (`application-test.properties`)

- используется in-memory H2;
- отключается Eureka (`eureka.client.enabled=false`);
- отключается автозапуск Kafka listeners (`spring.kafka.listener.auto-startup=false`).

---

## 5) Доменные модели

## `ProductDoc`

Elasticsearch документ `products`:
- `id`
- `name`
- `category`
- `price`
- `imageUrl`

## `UserAction`

Elasticsearch документ `user_actions`:
- `id`
- `userId`
- `productId`
- `actionType`

## `RecommendationResponse`

DTO ответа API:
- `products` (список `ProductDoc`)
- `currentPage`
- `totalElements`
- `hasNext`

## `ErrorResponse`

DTO для ошибок:
- `status`
- `message`
- `timestamp`

---

## 6) Репозитории

## `ProductSearchRepository`

Наследуется от `ElasticsearchRepository<ProductDoc, String>`.

Методы:
- `Page<ProductDoc> findByCategory(String category, Pageable pageable)`
- `List<ProductDoc> findAllByIdIn(List<String> ids)`
- плюс стандартные CRUD/поисковые методы `ElasticsearchRepository`.

## `ActionRepository`

Наследуется от `ElasticsearchRepository<UserAction, String>`.
Специализированных методов пока не добавлено.

---

## 7) Сервисный слой (бизнес-логика)

## `RecommendationService`

Ключевой сервис рекомендаций.

Зависимости:
- `ProductSearchRepository productRepository`
- `ElasticsearchOperations elasticsearchOperations`
- `RedisTemplate<String, Object> redisTemplate`

### Метод `getRecommendations(String userId, int page, int size)`

Алгоритм:
1. Читает ключ из Redis: `user:{userId}:fav_category`.
2. Если категория найдена:
   - выполняет `findByCategory(category, pageable)`.
3. Если категория не найдена:
   - выполняет `findAll(pageable)`.
4. Упаковывает результат в `RecommendationResponse`.

Особенности:
- помечен `@Cacheable(value = "recommendations", key = "#userId + #page", unless = "#result == null")`.
- Это кэш-аспект Spring Cache, но в проекте нет явного `@EnableCaching` и cache provider-конфигурации (см. раздел «Известные ограничения»).

### Метод `getPopularProducts(int limit)`

Алгоритм:
1. Строит Elasticsearch aggregation `terms` по полю `productId` в индексе `user_actions`.
2. Извлекает `limit` самых частых productId.
3. Загружает соответствующие продукты через `findAllByIdIn(popularIds)`.
4. Возвращает список популярных `ProductDoc`.

Важно:
- порядок результатов из `findAllByIdIn` не гарантирует порядок «по популярности»;
- метод корректно возвращает пустой список при отсутствии агрегаций или id.

## `ProductSyncService`

Метод `saveProduct(ProductDoc productDoc)`:
- сохраняет/обновляет документ в Elasticsearch;
- ловит исключение и логирует ошибку;
- исключение наружу не пробрасывает (поведение «best effort»).

---

## 8) Kafka consumers

## `ProductSyncConsumer`

- `@KafkaListener(topics = "product-updates", groupId = "rec-group")`
- получает `ProductDoc` из Kafka и передает в `ProductSyncService.saveProduct(...)`.

Назначение: онлайн-синхронизация каталога товаров в поисковый индекс.

## `EventConsumer`

- `@KafkaListener(topics = "user-actions", groupId = "rec-group", concurrency = "3")`
- принимает `UserAction`, сейчас только логирует действие.

Назначение: заготовка под обработку пользовательских событий (например, построение профиля пользователя, подсчет скоринга и т.д.).

---

## 9) REST API

Базовый путь: `/api/recommendations`

### 9.1 `GET /api/recommendations/{userId}`

Параметры query:
- `page` (default `0`)
- `size` (default `10`)

Ответ: `RecommendationResponse`

Пример:
```bash
curl "http://localhost:8099/api/recommendations/user_1?page=0&size=10"
```

### 9.2 `GET /api/recommendations/popular`

Параметры query:
- `limit` (default `10`)

Ответ: список `ProductDoc`.

### 9.3 CORS

Контроллер помечен `@CrossOrigin(origins = "*")`, т.е. доступ открыт для всех origin (удобно для разработки, но требует ужесточения в production).

---

## 10) Обработка ошибок

`GlobalExceptionHandler` (`@ControllerAdvice`) обрабатывает:

- `NoSuchIndexException` (Elasticsearch):
  - HTTP 500
  - message: `"Search service is temporality unavailable"` (орфография как в коде)
- `RedisConnectionFailureException`:
  - HTTP 503
  - message: `"Personalization data source error"`
- `Exception`:
  - HTTP 500
  - message: `"An internal server error occurred"`

Формат тела ошибки: `ErrorResponse`.

---

## 11) Frontend-страница (`static/index.html`)

Есть минималистичный UI для ручного теста:
- ввод `userId`;
- вызов API;
- рендер карточек товаров.

Сейчас в JS зашит `API_URL = 'http://localhost:8080/api/recommendations'`.
При стандартном запуске backend работает на `8026` (или `8099` снаружи в Docker), поэтому URL нужно синхронизировать под фактический порт.

---

## 12) Docker и локальная инфраструктура

`compose.yaml` поднимает:

- `elasticsearch` (внешний порт `9444 -> 9200`)
- `postgres` (`5499 -> 5432`)
- `redis` (`6399 -> 6379`)
- `zookeeper`
- `kafka` (`9099 -> 9098`, advertised listeners настроены на `kafka:9092` и `localhost:9099`)
- `recommendation-service` (`8099 -> 8026`)
- `k6` (выполняет `k6/load-test.js`)

Особенности:
- Для `elasticsearch/postgres/redis` есть `healthcheck`.
- `recommendation-service` стартует после `healthy` зависимостей.
- ENV для app прокидываются из compose.

### Dockerfile

Multi-stage:
1. `maven:3.9.6-eclipse-temurin-21` — сборка jar (`mvn clean package -DskipTests`)
2. `eclipse-temurin:21-jre` — runtime контейнер

Примечание:
- В `pom.xml` Java 17, а Docker образ использует Java 21. Обычно это совместимо (байткод 17 запускается на 21), но лучше унифицировать версии для предсказуемости.

---

## 13) CI/CD workflow

`.github/workflows/main.yml`:

1. Checkout
2. Setup JDK 17 + Maven cache
3. Build jar (`mvn clean package -DskipTests`)
4. `docker compose up -d --build`
5. Ожидание `/actuator/health` на `localhost:8099`
6. Запуск k6 (`docker compose run --name k6-test k6`)
7. Анализ логов `recommendation-service` на `Exception|Error`
8. Проверка exit code контейнера `k6-test`
9. `docker compose down` (always)

---

## 14) Тесты и покрытие сценариев

### `RecommendationServiceApplicationTests`
- проверка поднятия контекста (`@SpringBootTest`, profile `test`);
- Elasticsearch репозитории мокируются, чтобы контекст не требовал живого ES.

### `RecommendationServiceTest`
- проверяет две ветки `getRecommendations`:
  - с favorite category (персонализированный поиск),
  - без category (общий поиск по всем).

### `RecommendationControllerTest`
- `@WebMvcTest` для `GET /api/recommendations/{userId}`;
- проверка HTTP 200 и структуры JSON.

### `ProductSyncServiceTest`
- проверяет сохранение в репозиторий;
- проверяет «graceful handling» исключения при save.

### `ProductSyncConsumerTest`
- проверяет делегирование события в `ProductSyncService`.

### `GlobalExceptionHandlerTest`
- проверяет Redis fallback: 503 + expected message.

---

## 15) Основные потоки данных

## Поток A: выдача рекомендаций (REST)

1. Клиент вызывает `GET /api/recommendations/{userId}?page=&size=`.
2. `RecommendationController` делегирует в `RecommendationService`.
3. Сервис читает favorite category из Redis.
4. Делает запрос в Elasticsearch (`findByCategory` или `findAll`).
5. Возвращает `RecommendationResponse`.

## Поток B: синхронизация продуктов (Kafka -> ES)

1. Продюсер отправляет `ProductDoc` в топик `product-updates`.
2. `ProductSyncConsumer` получает сообщение.
3. `ProductSyncService.saveProduct` индексирует документ в `products`.

## Поток C: популярные товары

1. Клиент вызывает `GET /api/recommendations/popular?limit=N`.
2. `RecommendationService.getPopularProducts` агрегирует `user_actions` по `productId`.
3. По списку id получает товары из `products`.
4. Возвращает список популярных товаров.

---

## 16) Как запускать проект

## Вариант 1: полный стек в Docker

```bash
docker compose up -d --build
```

Проверка здоровья:
```bash
curl http://localhost:8099/actuator/health
```

Проверка API:
```bash
curl "http://localhost:8099/api/recommendations/user_1?page=0&size=10"
```

Остановка:
```bash
docker compose down
```

## Вариант 2: локально

Нужны PostgreSQL + Elasticsearch + Redis + Kafka.

```bash
mvn clean package
mvn spring-boot:run
```

API по умолчанию:
```text
http://localhost:8026
```

---

## 17) Observability и эксплуатация

- Health endpoint: `GET /actuator/health`
- Info/metrics: `GET /actuator/info`, `GET /actuator/metrics`
- Логирование:
  - app package: `WARN`
  - spring package: `WARN`
- В коде есть `info/debug/error` сообщения в ключевых точках сервисов/контроллеров/обработчиков ошибок.

---

## 18) Переменные окружения (фактически используемые)

- `SPRING_DATASOURCE_URL`
- `ELASTICSEARCH_URL`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `EUREKA_SERVER_URL`

Если не заданы — применяются fallback значения из `application.properties`/`application.yml`.

---

## 19) Известные ограничения и технические замечания

1. **Несоответствие портов между артефактами**
   - backend по умолчанию: `8026`;
   - Docker внешний: `8099`;
   - `static/index.html` обращается к `8080`.
   - Для корректной ручной проверки фронт-странице нужен актуальный URL.

2. **`@Cacheable` в `RecommendationService`**
   - аннотация есть, но в проекте не видно явного включения кэширования (`@EnableCaching`) и настроенного cache manager.
   - Без этого кеш может не работать ожидаемо.

3. **JPA/PostgreSQL зависимости подключены, но доменной JPA-модели нет**
   - сейчас основная бизнес-логика завязана на Elasticsearch/Redis/Kafka.

4. **`EventConsumer` пока только логирует**
   - обработка `user-actions` не влияет на рекомендации напрямую.

5. **Порядок популярных товаров**
   - после `findAllByIdIn` порядок не обязательно соответствует порядку агрегации популярности.

6. **Docker Java version**
   - сборка/рантайм в Docker на Java 21, проектный target Java 17.

7. **Безопасность/CORS**
   - `@CrossOrigin("*")` в production лучше ограничить доверенными доменами.

8. **Сервисный fallback при ошибке индексации**
   - `ProductSyncService.saveProduct` проглатывает исключение (логирует и продолжает), что полезно для отказоустойчивости, но может скрывать потерю данных без метрик/ретраев.

---

## 20) Рекомендации по дальнейшему развитию

- Включить и явно настроить Spring Cache (например, Redis cache manager), если кеш действительно нужен.
- Привести порты к единому стандарту во всех файлах (`README`, `index.html`, `compose`, CI comments).
- Реализовать полноценную обработку `user-actions`:
  - сохранение/обогащение профиля пользователя,
  - online/offline фичи для ранжирования.
- Добавить валидацию входных параметров (`page`, `size`, `limit`) и контракт ошибок для bad request.
- Добавить observability:
  - бизнес-метрики (latency, hit/miss personalization, indexing failures),
  - structured logs, tracing.
- Дополнить тесты:
  - интеграционные с Testcontainers (ES, Redis, Kafka),
  - тест endpoint `/popular`,
  - тесты негативных сценариев API.
- Ужесточить CORS и добавить security baseline.

---

## 21) Краткая ментальная модель проекта

Если объяснить очень коротко:

- каталог товаров живет в Elasticsearch;
- персонализация берется из Redis по ключу любимой категории;
- API выдает либо товары любимой категории, либо общий список;
- Kafka обновляет документы товаров в Elasticsearch;
- отдельный endpoint строит популярные товары через агрегацию пользовательских событий;
- все это можно поднять одной командой через Docker Compose и прогнать нагрузку k6.

Именно так сейчас «дышит» весь проект end-to-end.
