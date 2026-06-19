# Observability (Grafana + Loki)

This project ships a local observability stack for **metrics** (Prometheus) and **logs** (Loki + Promtail), with Grafana pre-provisioned as the entry point.

## Quick start (full stack)

From the project root:

```bash
docker compose up -d
```

This starts PostgreSQL, Redis, Elasticsearch, Kafka, the Recommendation Service, and the observability stack (Loki, Promtail, Prometheus, Grafana).

Optional: set `OPENAI_API_KEY` in a `.env` file (see `.env.example`) before starting if you need AI features.

Wait until the app is healthy:

```bash
docker compose ps
curl http://localhost:8026/actuator/health
```

## Grafana access

| Setting | Value |
|---|---|
| URL | http://localhost:3000 |
| Username | `admin` |
| Password | `admin` |

Change the default password after first login if the stack is exposed beyond localhost.

## Viewing logs

### Pre-built dashboards

Open **Dashboards → Recommendation** in Grafana:

- **Recommendation Service Logs** — log volume, filters by level and `correlationId`
- **Recommendation Service Overview** — metrics plus a live log panel

### Explore (ad-hoc queries)

1. Open **Explore** (compass icon).
2. Select the **Loki** datasource.
3. Run LogQL, for example:

```logql
{job="recommendation-service"}
```

Parse JSON fields (correlation ID, level, logger):

```logql
{job="recommendation-service"} | json
```

Filter errors only:

```logql
{job="recommendation-service"} | json | level="ERROR"
```

Trace a single request:

```logql
{job="recommendation-service"} | json | correlationId="your-correlation-id-here"
```

## Log format

When the `docker` or `prod` Spring profile is active, the app emits **JSON logs** to stdout (via Logback + Logstash encoder). Each line includes:

- `timestamp`, `level`, `logger`, `message`
- `correlationId` and `requestId` from `CorrelationIdFilter` (MDC)
- `service` — application name

Promtail ships container stdout to Loki. The Compose service name becomes the Loki `job` label (`recommendation-service`).

Local `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` uses human-readable console logs instead.

## Observability-only mode

If you run the app locally (or only need monitoring containers):

```bash
docker compose -f docker-compose.observability.yml up -d
```

Prometheus scrapes `recommendation-service:8026` via `host-gateway`, so the app must listen on port **8026** on your machine.

## Stack components

| Service | Port | Role |
|---|---|---|
| Grafana | 3000 | UI, dashboards, Explore |
| Loki | 3100 | Log storage and query API |
| Promtail | — | Ships Docker container logs to Loki |
| Prometheus | 9090 | Metrics scraping |
| Alertmanager | 9093 | Alert routing (optional) |

Config lives under `monitoring/`:

- `monitoring/loki/loki-config.yml`
- `monitoring/promtail/promtail-config.yml`
- `monitoring/grafana/provisioning/` — datasources and dashboard loader
- `monitoring/grafana/dashboards/` — JSON dashboards
- `monitoring/prometheus/prometheus.yml` — scrape targets

## Troubleshooting

**No logs in Grafana**

- Confirm Promtail is running: `docker compose ps promtail`
- Generate traffic: `curl http://localhost:8026/actuator/health`
- In Explore, widen the time range and try `{job=~".+"}` to see all containers

**App container not healthy**

- Check logs: `docker compose logs recommendation-service`
- Elasticsearch and Kafka can take 1–2 minutes on first start

**Prometheus target down (observability-only)**

- Ensure the app is running on port 8026
- On Linux, `host-gateway` maps to the host; on some setups you may need to set the scrape target to `host.docker.internal:8026` in `monitoring/prometheus/prometheus.yml`
