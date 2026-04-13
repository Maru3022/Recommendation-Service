## Recommendation Service

Recommendation Service — это высокопроизводительный микросервис на Spring Boot, который отдаёт персональные рекомендации товаров.
Он использует PostgreSQL, Elasticsearch, Redis и Kafka для синхронизации данных.

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

- **Готовый CI/CD**
  - GitHub Actions:
    - собирает JAR;
    - запускает `mvn test` с H2 (in‑memory) для тестов;
    - собирает и публикует Docker-образ.

---

### Технологии

- **Backend**: Java 17, Spring Boot 3 (`spring-boot-starter-web`, Data JPA, Data Redis, Data Elasticsearch, Spring Kafka)
- **Хранилища**: PostgreSQL, Elasticsearch, Redis
- **Сообщения**: Kafka
- **Инфраструктура**: Docker, GitHub Actions

---

## Быстрый старт

### Локальный запуск

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

### Запуск тестов

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

3. **Docker Build & Push**
   - Собирает Docker-образ;
   - Публикует в GitHub Container Registry (GHCR).

4. **Deploy**
   - Staging и Production деплой (настраивается через environments).

Благодаря этому репозиторий можно использовать как готовый пример production‑подобного recommendation‑сервиса с CI/CD.
