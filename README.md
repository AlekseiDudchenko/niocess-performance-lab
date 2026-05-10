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
