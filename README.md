# Niocess Performance Lab

Benchmark lab for demonstrating how Niocess can reduce duplicate backend calls, protect slow downstream services, and improve latency under repeated expensive requests.

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

With Niocess layer later:

```text
JMeter/k6 -> client-app -> Niocess Layer -> external-api-simulator
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

```bash
curl http://localhost:8080/api/db/pricing      # random product from PostgreSQL
curl http://localhost:8080/api/db/risk-score   # random risk profile from PostgreSQL
```

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

External-API mode (original):

```text
JMeter/k6 -> client-app -> external-api-simulator
```

Database mode (new):

```text
JMeter/k6 -> client-app -> PostgreSQL
```
