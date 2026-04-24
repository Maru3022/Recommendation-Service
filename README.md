# Recommendation Service

Recommendation Service — микросервис на Spring Boot для выдачи персональных и популярных товарных рекомендаций.
Сервис использует PostgreSQL, Elasticsearch, Redis и Kafka, а теперь подготовлен не только для Docker, но и для полноценного запуска в Kubernetes.

## Что умеет сервис

- `GET /api/recommendations/{userId}?page={page}&size={size}` возвращает персональные рекомендации.
- `GET /api/recommendations/popular?limit={limit}` возвращает популярные товары.
- Персонализация строится через Redis по ключу `user:{userId}:fav_category`.
- Данные товаров синхронизируются через Kafka consumer `product-updates`.
- Метрики Prometheus и health endpoints доступны через Spring Boot Actuator.
- Поддержаны readiness/liveness probes и graceful shutdown для Kubernetes rollout.

## Технологии

- Java 21
- Spring Boot 3
- Spring Data JPA
- Spring Data Elasticsearch
- Spring Data Redis
- Spring Kafka
- PostgreSQL
- Docker
- Kubernetes + Kustomize overlays
- GitHub Actions CI/CD

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
