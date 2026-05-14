# Performance Lab

Benchmark lab for measuring latency and throughput across different backend scenarios.

## Plan

### Phase 1 — Baseline (done)
- [x] `client-app` — Spring Boot REST API with configurable `delayMs` per request
- [x] `external-api-simulator` — slow/paid third-party API simulator
- [x] PostgreSQL support — DB benchmark mode with seeded data (500 products, 500 risk profiles)
- [x] Docker Compose stack — single `docker compose up --build` starts all three services
- [x] JMeter baseline experiment — matrix of 6 thread levels × 6 delay levels × 3 repeats
- [x] Experiment tooling — `run-baseline-experiment.ps1` runner + `analyze-baseline-experiment.py` analyzer
- [x] Baseline results saved — response time summaries and boxplot for 200 vs 2000 threads

### Phase 2 — pgasync (done)
- [x] Non-blocking PostgreSQL driver wired up via [postgres-async-driver](https://github.com/AlekseiDudchenko/postgres-async-driver) fork
- [x] `?driver=pgasync` path on all `/api/db/*` endpoints — Tomcat thread released during I/O (Netty handles the query asynchronously)
- [x] `?driver=jdbc` path unchanged — blocking Spring Data JPA / Hibernate

### Phase 3 — Analysis & Demo
- [ ] Side-by-side comparison charts (before vs after)
- [ ] Document observed improvements per scenario (high concurrency, slow downstream, repeated identical requests)
- [ ] Prepare demo script / slide deck

## Structure

```text
apps/
  client-app/
    Spring Boot REST API used by load tests
  external-api-simulator/
    Slow/paid third-party API simulator
```

## Run

```bash
docker compose up --build
```

Services:

- Client app: `http://localhost:8080`
- External API simulator: `http://localhost:8090`

## Demo Flow

Direct baseline:

```text
JMeter/k6 -> client-app -> external-api-simulator
```


## Useful Requests

```bash
curl "http://localhost:8080/api/pricing?productId=123&delayMs=500"
curl "http://localhost:8080/api/risk-score?userId=456&delayMs=500"
curl "http://localhost:8080/api/analytics/external-report?tenantId=123&period=30d&delayMs=1000"
curl "http://localhost:8090/metrics"
```

## PostgreSQL — Database Benchmark Mode

A PostgreSQL database runs alongside the lab environment for database-focused benchmark runs.
Use the `/api/db/*` endpoints to measure DB query latency instead of HTTP call latency.

### Start

```bash
docker compose up --build
```

PostgreSQL starts automatically with the rest of the stack. The schema and seed data (500 products, 500 risk profiles) are applied on the first start via `db/init/`.

### DB Endpoints

Both endpoints accept an optional `?driver=` parameter to switch between drivers:

| `driver` | Description |
|----------|-------------|
| `jdbc` (default) | Blocking — Spring Data JPA / Hibernate, Tomcat thread held for the duration |
| `pgasync` | Non-blocking — [postgres-async-driver](https://github.com/AlekseiDudchenko/postgres-async-driver), Tomcat thread released while Netty handles I/O |

```bash
# JDBC (default)
curl "http://localhost:8080/api/db/pricing"
curl "http://localhost:8080/api/db/risk-score"

# Explicit JDBC
curl "http://localhost:8080/api/db/pricing?driver=jdbc"
curl "http://localhost:8080/api/db/risk-score?driver=jdbc"

# pgasync — non-blocking
curl "http://localhost:8080/api/db/pricing?driver=pgasync"
curl "http://localhost:8080/api/db/risk-score?driver=pgasync"
```

**Response shape** (same for both drivers):

```json
// GET /api/db/pricing
{ "id": 42, "sku": "SKU-042", "name": "Widget", "price": 9.99, "currency": "USD" }

// GET /api/db/risk-score
{ "id": 7, "clientId": "client-007", "score": 82, "category": "HIGH" }
```

Status codes: `200 OK`, `404 Not Found` (empty table), `400 Bad Request` (invalid driver value).

### Configuration

| Environment Variable | Default                                    | Description              |
|----------------------|--------------------------------------------|--------------------------|
| `POSTGRES_DB`        | `perflab`                                  | Database name            |
| `POSTGRES_USER`      | `perflab`                                  | PostgreSQL username       |
| `POSTGRES_PASSWORD`  | `perflab`                                  | PostgreSQL password       |
| `DB_URL`             | `jdbc:postgresql://localhost:5432/perflab` | JDBC URL for client-app  |
| `DB_USER`            | `perflab`                                  | DB user for client-app   |
| `DB_PASSWORD`        | `perflab`                                  | DB password for client-app|

Override defaults in a `.env` file next to `docker-compose.yml`:

```dotenv
POSTGRES_DB=perflab
POSTGRES_USER=perflab
POSTGRES_PASSWORD=strongpassword
```

### Benchmark Flows

External-API mode:

```text
JMeter/k6 -> client-app -> external-api-simulator
```

Database mode — JDBC (blocking):

```text
JMeter/k6 -> client-app (Tomcat thread held) -> PostgreSQL
```

Database mode — pgasync (non-blocking):

```text
JMeter/k6 -> client-app (Tomcat thread released) -> Netty -> PostgreSQL
```
