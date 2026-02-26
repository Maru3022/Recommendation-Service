## Recommendation Service

Recommendation Service — это высокопроизводительный микросервис на Spring Boot, который отдаёт персональные рекомендации товаров.  
Он использует PostgreSQL, Elasticsearch, Redis, Kafka и нагрузочное тестирование через k6, полностью упакован в Docker Compose и имеет готовый CI/CD на GitHub Actions.

---

### Возможности

- **REST API для рекомендаций**  
  - `GET /api/recommendations/{userId}?page={page}&size={size}`  
  - Возвращает объект `RecommendationResponse` с:
    - списком товаров;
    - текущей страницей;
    - общим количеством элементов;
    - флагом `hasNext` (есть ли следующая страница).

- **Персонализация через Redis**  
  - Любимая категория пользователя читается из ключа `user:{userId}:fav_category` в Redis.  
  - Если категория есть → поиск только по этой категории в Elasticsearch.  
  - Если категории нет → дефолтный поиск по всем товарам.

- **Поиск и хранение в Elasticsearch**  
  - `ProductSearchRepository` — поиск товаров.  
  - `ActionRepository` — хранение пользовательских действий (просмотры, клики и т.п.).  
  - Индексы создаются автоматически через Spring Data Elasticsearch.

- **Синхронизация товаров из Kafka**  
  - `ProductSyncConsumer` слушает топик `product-updates` (группа `rec-group`).  
  - Каждый полученный `ProductDoc` сохраняется в Elasticsearch через `ProductSyncService`.

- **Обработка ошибок и устойчивость**  
  - `GlobalExceptionHandler`:
    - проблемы с Elasticsearch (нет индекса) → HTTP `500`;  
    - падение Redis → HTTP `503` и сообщение _"Personalization data source error"_;  
    - любые неожиданные ошибки → HTTP `500` с безопасным текстом.

- **Нагрузочное тестирование через k6**  
  - Скрипт `k6/load-test.js` шлёт запросы на `GET /api/recommendations/user_{id}` со стадиями нагрузки:
    - `30s` → `50` виртуальных пользователей;  
    - `1m` → `100` пользователей;  
    - `30s` — плавное снижение нагрузки.  
  - В Docker запускается отдельный сервис `k6`, который автоматически выполняет сценарий.

- **Готовый CI/CD**  
  - GitHub Actions:
    - собирает JAR;
    - запускает `mvn test` с H2 (in‑memory) для тестов;
    - поднимает весь Docker‑стек и прогоняет сценарий k6.

---

### Технологии

- **Backend**: Java 17, Spring Boot 3 (`spring-boot-starter-web`, Data JPA, Data Redis, Data Elasticsearch, Spring Kafka)  
- **Хранилища**: PostgreSQL, Elasticsearch, Redis  
- **Сообщения**: Kafka + Zookeeper  
- **Нагрузочное тестирование**: k6  
- **Инфраструктура**: Docker, Docker Compose, GitHub Actions

---

## Быстрый старт

### 1. Запуск всего проекта через Docker (рекомендуется)

**Требования**:
- установлен Docker;
- Docker Compose v2 (команда `docker compose`).

Из корня проекта выполните:

```bash
docker compose up -d --build
```

Будут запущены контейнеры:
- **`postgres`** — основная БД (`recommendation_db`);  
- **`elasticsearch`** — поисковый движок;  
- **`redis`** — кеш/хранилище данных для персонализации;  
- **`zookeeper` + `kafka`** — брокер сообщений для обновлений товаров;  
- **`recommendation-service`** — Spring Boot сервис на порту `8026`;  
- **`k6`** — контейнер, автоматически запускающий `k6/load-test.js`.

Проверить работу API:

```bash
curl "http://localhost:8026/api/recommendations/user_1?page=0&size=10"
```

Посмотреть логи k6:

```bash
docker compose logs k6 --tail=100
```

Остановить все сервисы:

```bash
docker compose down
```

---

### 2. Локальный запуск без Docker (для разработки)

**Требования**:
- JDK 17+;
- Maven;
- запущенные PostgreSQL, Elasticsearch, Redis и Kafka  
  (либо адаптировать `src/main/resources/application.properties` под своё окружение).

Шаги:

```bash
mvn clean package
mvn spring-boot:run
```

Сервис будет доступен по адресу:

```text
http://localhost:8026
```

Основной эндпоинт:

```text
GET /api/recommendations/{userId}?page=0&size=10
```

Пример запроса:

```bash
curl "http://localhost:8026/api/recommendations/some_user?page=0&size=10"
```

---

### 3. Запуск тестов

- **Юнит‑ и интеграционные тесты** (in‑memory H2):

```bash
mvn test
```

Тестовый профиль:
- использует H2 вместо PostgreSQL;  
- мокает Elasticsearch‑репозитории и отключает Kafka‑листенеры;  
- позволяет гонять тесты без внешних сервисов.

---

## CI/CD (GitHub Actions)

Workflow описан в `.github/workflows/main.yml` и делает следующее:

1. **Checkout и настройка JDK**  
   - `actions/checkout@v4`;  
   - `actions/setup-java@v4` c Java 17.

2. **Сборка приложения**  
   - `mvn clean package -DskipTests`.

3. **Инфраструктура и нагрузочные тесты**  
   - `docker compose up -d --build` — поднимает весь стек;  
   - делает паузу, чтобы сервисы успели стартовать;  
   - запускает сценарий k6 внутри контейнера `k6`.

4. **Проверка результата**  
   - инспектирует код выхода контейнера `k6`;  
   - если код не `0` — pipeline помечается как неуспешный.

Благодаря этому репозиторий можно использовать как готовый пример production‑подобного recommendation‑сервиса с полной инфраструктурой и CI/CD.
