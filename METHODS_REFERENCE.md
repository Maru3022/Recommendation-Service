# Справочник методов микросервиса Recommendation-Service

Документ описывает **каждый явный метод** в `src/main/java`, а также **сгенерированные или унаследованные** методы репозиториев и данных моделей (Lombok), чтобы было понятно, что вызывается извне и с какими побочными эффектами.

Общий пакет: `com.example.recommendationservice`.

---

## 1. `RecommendationServiceApplication`

Файл: `RecommendationServiceApplication.java`

### `public static void main(String[] args)`

**Назначение.** Точка входа Spring Boot-приложения.

**Поведение.** Вызывает `SpringApplication.run(RecommendationServiceApplication.class, args)`, что:

- инициализирует контекст Spring;
- поднимает веб-сервер (Tomcat), настроенный в `application.properties` (`server.port`, лимиты потоков);
- регистрирует автоконфигурации для JPA, Redis, Elasticsearch, Kafka и т.д.

**Параметры.**

- `args` — аргументы командной строки (стандартные для Spring Boot: профили, переопределение свойств).

**Возвращаемое значение.** Нет (`void`).

**Исключения.** Любые фатальные ошибки при старте контекста приведут к завершению процесса с ненулевым кодом.

---

## 2. Конфигурация: `ElasticsearchConfig`

Файл: `ElasticsearchConfig.java`

**Явных методов нет** — класс помечен `@Configuration` и `@EnableElasticsearchRepositories(basePackages = "com.example.recommendationservice.repository")`.

**Эффект.** Включает сканирование и создание Spring Data Elasticsearch-репозиториев в указанном пакете (например, `ProductSearchRepository`, `ActionRepository`).

---

## 3. Конфигурация: `KafkaConfig`

Файл: `KafkaConfig.java`

**Явных методов нет** — `@Configuration` + `@EnableKafka`.

**Эффект.** Включает обработку `@KafkaListener` в потребителях (`ProductSyncConsumer`, `EventConsumer`).

---

## 4. Конфигурация: `RedisConfig`

Файл: `RedisConfig.java`

### `public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory)`

**Назначение.** Создаёт бин `RedisTemplate<String, Object>` для операций с Redis.

**Параметры.**

- `connectionFactory` — фабрика соединений, предоставляемая Spring Boot из `spring.data.redis.*`.

**Поведение.**

1. Создаёт `RedisTemplate`, привязывает `connectionFactory`.
2. **Ключи (и hash keys):** `StringRedisSerializer` — строки как UTF-8.
3. **Значения (и hash values):** `GenericJackson2JsonRedisSerializer` — сериализация значений в JSON.

**Возвращает.** Настроенный `RedisTemplate`.

**Побочные эффекты.** Нет (только создание бина).

**Использование в сервисе.** `RecommendationService` читает строковые значения через `redisTemplate.opsForValue().get(...)`; при записи из другого сервиса важно, чтобы тип значения был совместим с десериализацией Jackson.

---

## 5. Сервис: `RecommendationService`

Файл: `RecommendationService.java`

Зависимости (через конструктор Lombok `@RequiredArgsConstructor`):

- `ProductSearchRepository productRepository`
- `ElasticsearchOperations elasticsearchOperations`
- `RedisTemplate<String, Object> redisTemplate`

---

### `public RecommendationResponse getRecommendations(String userId, int page, int size)`

**Назначение.** Сформировать страницу рекомендаций товаров для пользователя.

**Аннотации.**

- `@Cacheable(value = "recommendations", key = "#userId + #page", unless = "#result == null")` — кэш Spring Cache по ключу `userId + page` (работоспособность кэша зависит от наличия `@EnableCaching` и провайдера кэша в приложении).

**Алгоритм.**

1. Логирует запрос на уровне `DEBUG`.
2. Читает из Redis ключ: `"user:" + userId + ":fav_category"`. Ожидается строка категории (или `null`, если ключа нет).
3. Строит `Pageable` через `PageRequest.of(page, size)`.
4. Если категория **не** `null`:
   - вызывает `productRepository.findByCategory(favoriteCategory, pageable)` — выборка товаров **только** из этой категории в индексе `products`.
5. Если категория `null`:
   - вызывает `productRepository.findAll(pageable)` — постраничная выборка **всех** товаров.
6. Упаковывает результат в `RecommendationResponse`:
   - `products` — содержимое страницы;
   - `currentPage` — номер страницы из `Page`;
   - `totalElements` — общее число элементов по запросу;
   - `hasNext` — есть ли следующая страница.

**Параметры.**

- `userId` — идентификатор пользователя (произвольная строка, используется в ключе Redis).
- `page` — номер страницы (0-based, как в Spring Data).
- `size` — размер страницы.

**Возвращает.** `RecommendationResponse` (не `null` при нормальном завершении; может содержать пустой список товаров).

**Исключения (типичные).**

- Ошибки Redis → могут привести к `RedisConnectionFailureException`, который обрабатывается `GlobalExceptionHandler` (HTTP 503).
- Ошибки Elasticsearch (нет индекса и т.п.) → `NoSuchIndexException` или другие → частично перехватывается глобальным обработчиком для `NoSuchIndexException` (HTTP 500).

**Замечания.**

- Ключ Redis **не** включает `size`; при смене `size` для той же пары `userId`/`page` кэш может вернуть несоответствующий размер страницы — это ограничение текущей конфигурации `@Cacheable`.

---

### `public List<ProductDoc> getPopularProducts(int limit)`

**Назначение.** Вернуть список «популярных» товаров по частоте `productId` в индексе `user_actions`.

**Алгоритм.**

1. Строит `NativeQuery`:
   - `withMaxResults(0)` — в выборку попадут только агрегации, не сами документы.
   - Агрегация `terms` с именем `"most_popular"`:
     - поле `"productId"`;
     - `size(limit)` — максимум «ведёр» в terms (верхние по частоте значения).
2. Выполняет `elasticsearchOperations.search(query, UserAction.class)` — маппинг по модели `UserAction` и индексу `user_actions`.
3. Если агрегаций нет → возвращает пустой `ArrayList`.
4. Приводит агрегации к `ElasticsearchAggregations`, достаёт bucket’ы `sterms`, собирает `popularIds` (строковые ключи).
5. Если список id пуст → пустой список.
6. Загружает товары: `productRepository.findAllByIdIn(popularIds)`.

**Параметры.**

- `limit` — верхняя граница числа популярных productId в агрегации (не проверяется на отрицательные значения в коде).

**Возвращает.** `List<ProductDoc>` — может быть пустым.

**Исключения.** Любые сбои ES/сети пробрасываются вверх (кроме случаев, обработанных глобально) или приведут к 500 через `handleGenericException`.

**Замечания.**

- Порядок элементов в ответе **не гарантируется** совпадать с порядком популярности: `findAllByIdIn` не сортирует по score или позиции в агрегации.
- Требуется корректный маппинг поля `productId` в индексе `user_actions` как тип, подходящий для `terms` (обычно `keyword`).

---

## 6. Сервис: `ProductSyncService`

Файл: `ProductSyncService.java`

### `public void saveProduct(ProductDoc productDoc)`

**Назначение.** Индексировать или обновить документ товара в Elasticsearch.

**Поведение.**

1. В `try`: вызывает `productSearchRepository.save(productDoc)` — upsert по id в индекс `products` (см. `@Document` на `ProductDoc`).
2. При успехе: лог `DEBUG` с id.
3. При любом `Exception`: лог `ERROR` с id и сообщением; **исключение не пробрасывается**.

**Параметры.**

- `productDoc` — сущность товара; `id` используется как идентификатор документа в ES.

**Возвращаемое значение.** Нет.

**Исключения наружу.** Не выбрасывает — «тихий» сбой при ошибке ES.

**Побочные эффекты.** Запись в Elasticsearch при успехе.

---

## 7. Контроллер: `RecommendationController`

Файл: `RecommendationController.java`

Базовый путь: `@RequestMapping("/api/recommendations")`  
CORS: `@CrossOrigin(origins = "*")`

---

### `public ResponseEntity<RecommendationResponse> getRecommendations(String userId, int page, int size)`

**HTTP.** `GET /api/recommendations/{userId}`

**Параметры.**

- `userId` (`@PathVariable`) — из пути.
- `page` (`@RequestParam`, по умолчанию `0`).
- `size` (`@RequestParam`, по умолчанию `10`).

**Поведение.**

1. Логирует входящий запрос.
2. Замеряет время и вызывает `recommendationService.getRecommendations(userId, page, size)`.
3. Логирует длительность и число товаров в ответе.
4. Возвращает `200 OK` с телом `RecommendationResponse`.

**Возвращает.** `ResponseEntity` с телом и статусом 200 при успехе.

**Ошибки.** Перехватываются `GlobalExceptionHandler` в зависимости от типа исключения.

---

### `public ResponseEntity<List<ProductDoc>> getPopular(int limit)`

**HTTP.** `GET /api/recommendations/popular`

**Параметры.**

- `limit` (`@RequestParam`, по умолчанию `10`).

**Поведение.** Делегирует в `recommendationService.getPopularProducts(limit)`, возвращает `200 OK` со списком `ProductDoc`.

---

## 8. Глобальная обработка ошибок: `GlobalExceptionHandler`

Файл: `GlobalExceptionHandler.java`

Класс помечен `@ControllerAdvice` — методы применяются ко всем контроллерам в приложении.

Общий формат ответа при ошибках: `ErrorResponse(status, message, timestamp)`.

---

### `public ResponseEntity<ErrorResponse> handleElasticsearchException(Exception ex)`

**Срабатывает на.** `org.springframework.data.elasticsearch.NoSuchIndexException` (и подтипы, если есть).

**HTTP.** `500 Internal Server Error`

**Тело.** `status = 500`, `message = "Search service is temporality unavailable"` (текст как в коде).

**Логирование.** `ERROR` с деталями `ex.getMessage()`.

---

### `public ResponseEntity<ErrorResponse> handleRedisException(Exception ex)`

**Срабатывает на.** `org.springframework.data.redis.RedisConnectionFailureException`.

**HTTP.** `503 Service Unavailable`

**Тело.** `status = 503`, `message = "Personalization data source error"`.

**Логирование.** `ERROR` с пометкой о возможном отключении персонализации.

---

### `public ResponseEntity<ErrorResponse> handleGenericException(Exception ex)`

**Срабатывает на.** Любое необработанное `Exception` (резервный обработчик).

**HTTP.** `500 Internal Server Error`

**Тело.** `status = 500`, `message = "An internal server error occurred"`.

**Логирование.** `ERROR` с полным стектрейсом (`log.error("...", ex)`).

**Важно.** Для исключений, уже перехваченных более специфичными `@ExceptionHandler` с совместимым типом, Spring выберет наиболее специфичный обработчик; порядок объявления в классе и иерархия типов определяют совпадение.

---

## 9. Kafka: `ProductSyncConsumer`

Файл: `ProductSyncConsumer.java`

### `public void consumeProductUpdate(ProductDoc product)`

**Назначение.** Обработчик сообщений из Kafka.

**Аннотация.** `@KafkaListener(topics = "product-updates", groupId = "rec-group")`

**Параметры.**

- `product` — десериализуется из JSON в `ProductDoc` (конфигурация сериализации задаётся Spring Kafka по умолчанию / настройкам приложения).

**Поведение.** Вызывает `productSyncService.saveProduct(product)`.

**Побочные эффекты.** Запись в Elasticsearch через сервис (или тихий сбой при ошибке внутри `saveProduct`).

**Идемпотентность.** Повторная доставка того же сообщения приведёт к повторному `save` с тем же id — поведение как upsert в ES.

---

## 10. Kafka: `EventConsumer`

Файл: `EventConsumer.java`

### `public void consumeUserAction(UserAction action)`

**Назначение.** Обработчик пользовательских событий из Kafka.

**Аннотация.** `@KafkaListener(topics = "user-actions", groupId = "rec-group", concurrency = "3")`

**Параметры.**

- `action` — `UserAction` (id, userId, productId, actionType).

**Поведение.** Только логирование на уровне `INFO`: тип действия и userId.

**Побочные эффекты.** Нет записи в БД/ES в текущей реализации — задел под дальнейшую обработку.

---

## 11. Репозиторий: `ProductSearchRepository`

Файл: `ProductSearchRepository.java`

Интерфейс extends `ElasticsearchRepository<ProductDoc, String>`.

### Объявленные методы интерфейса

#### `Page<ProductDoc> findByCategory(String category, Pageable pageable)`

**Назначение.** Spring Data строит запрос по имени метода: поиск документов `ProductDoc`, у которых поле `category` равно переданному значению, с пагинацией.

**Параметры.**

- `category` — значение поля `category` (в маппинге `Keyword`).
- `pageable` — размер и номер страницы.

**Возвращает.** `Page<ProductDoc>`.

---

#### `List<ProductDoc> findAllByIdIn(List<String> ids)`

**Назначение.** Загрузка товаров по списку идентификаторов.

**Параметры.** `ids` — список id документов в индексе `products`.

**Возвращает.** Список найденных `ProductDoc` (несуществующие id обычно просто отсутствуют в результате).

---

### Унаследованные методы `ElasticsearchRepository<ProductDoc, String>`

Ниже — типичные методы, которые доступны вызывающему коду через интерфейс (реализация генерируется Spring Data). Точный набор соответствует версии Spring Data Elasticsearch в проекте:

- `save(S entity)`, `saveAll(Iterable<S> entities)`
- `findById(ID id)`, `findAll()`, `findAll(Pageable pageable)`
- `existsById(ID id)`
- `count()`
- `deleteById(ID id)`, `delete(T entity)`, `deleteAll()`

и другие методы базовых интерфейсов `CrudRepository` / `PagingAndSortingRepository` / `ElasticsearchRepository`.

**Использование в коде.** Явно вызываются `findByCategory`, `findAll(Pageable)`, `save` (через `ProductSyncService`), `findAllByIdIn`.

---

## 12. Репозиторий: `ActionRepository`

Файл: `ActionRepository.java`

Интерфейс extends `ElasticsearchRepository<UserAction, String>`.

**Дополнительных методов в интерфейсе нет.**

**Унаследованные методы** — те же категории, что у `ElasticsearchRepository`: CRUD, пагинация, поиск по id и т.д.

**Использование в коде.** В основном коде сервисов прямых вызовов нет; репозиторий может использоваться в тестах (`@MockBean`) и потенциально для будущей логики (например, записи `UserAction` из `EventConsumer`).

---

## 13. Модели данных (Lombok)

Файлы: `ProductDoc`, `UserAction`, `RecommendationResponse`, `ErrorResponse`.

Аннотации Lombok (`@Data`, `@AllArgsConstructor`, `@NoArgsConstructor` где указано) **генерируют** стандартные методы:

- геттеры и сеттеры для всех полей;
- `equals`, `hashCode`, `toString` (для `@Data`);
- конструкторы по аннотациям конструкторов.

Ниже перечислены **поля и назначение сущностей**; отдельное перечисление каждого `getXxx`/`setXxx` не дублируется — они однозначно следуют из имён полей.

### `ProductDoc`

- **Индекс Elasticsearch:** `products` (`@Document(indexName = "products")`)
- **Поля:** `id`, `name`, `category` (Text/Keyword по аннотациям), `price`, `imageUrl`
- **Роль.** Документ каталога для поиска и выдачи рекомендаций.

### `UserAction`

- **Индекс Elasticsearch:** `user_actions`
- **Поля:** `id`, `userId`, `productId`, `actionType`
- **Роль.** События пользователя; используется в агрегации популярности по `productId`.

### `RecommendationResponse`

- **Поля:** `products`, `currentPage`, `totalElements`, `hasNext`
- **Роль.** DTO ответа REST для пагинированных рекомендаций.

### `ErrorResponse`

- **Поля:** `status`, `message`, `timestamp`
- **Роль.** Единый формат тела при ошибках из `GlobalExceptionHandler`.

---

## 14. Сводная таблица: метод → триггер

| Метод | Кто вызывает | Внешний триггер |
|--------|----------------|-----------------|
| `main` | JVM | Запуск приложения |
| `redisTemplate` | Spring | Инициализация контекста |
| `getRecommendations` (service) | `RecommendationController` | HTTP GET `/api/recommendations/{userId}` |
| `getPopularProducts` | `RecommendationController` | HTTP GET `/api/recommendations/popular` |
| `saveProduct` | `ProductSyncConsumer` | Kafka `product-updates` |
| `consumeProductUpdate` | Spring Kafka | Kafka `product-updates` |
| `consumeUserAction` | Spring Kafka | Kafka `user-actions` |
| `handleElasticsearchException` | Spring MVC | `NoSuchIndexException` |
| `handleRedisException` | Spring MVC | `RedisConnectionFailureException` |
| `handleGenericException` | Spring MVC | Прочие `Exception` |

---

## 15. Файл документации проекта целиком

Общая архитектура, конфигурация, Docker и CI/CD описаны в `PROJECT_DOCUMENTATION.md`. Этот файл (`METHODS_REFERENCE.md`) дополняет его детализацией **по методам и контрактам**.
