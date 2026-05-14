# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

A benchmark lab for measuring latency and throughput under different backend strategies. Two runnable services plus a PostgreSQL database, all wired together via Docker Compose.

## Running the stack

```bash
docker compose up --build   # start everything
docker compose down         # stop
```

Services after startup:
- `client-app` → http://localhost:8080
- `external-api-simulator` → http://localhost:8090
- PostgreSQL → localhost:5432 (db/user/pass: `perflab`)

## client-app (Spring Boot, Java 21)

Build and test:
```bash
cd apps/client-app
./gradlew build          # compile + test
./gradlew test           # tests only
./gradlew -x test bootJar  # build jar, skip tests (used by Dockerfile)
```

Run a single test class:
```bash
./gradlew test --tests "com.niocess.perflab.client.db.DbBenchmarkControllerTest"
```

Tests use `@WebMvcTest` + Mockito — no database or Spring context needed.

### Architecture

```
BenchmarkController        → proxies to external-api-simulator via ExternalApiClient (RestClient)
DbBenchmarkController      → queries PostgreSQL via Spring Data JPA repositories
ClientAppApplication       → registers RestClient bean; reads external-api.base-url from config
```

DB entities: `Product` (products table) and `RiskProfile` (risk_profiles table).  
Schema and seed data live in `db/init/01-schema.sql` and `02-seed.sql` (500 rows each).  
Config is in `src/main/resources/application.yml`; all values are env-overridable.

## external-api-simulator (Node.js 20, ESM)

```bash
cd apps/external-api-simulator
node --test          # run all tests (no install needed — zero dependencies)
node src/server.js   # run locally
```

Simulates slow/paid third-party APIs (`/external/pricing`, `/external/risk-score`, etc.).  
Delay is controlled via `EXTERNAL_API_DELAY_MS` env var or per-request `?delayMs=` query param.  
Exposes `/metrics` and `POST /metrics/reset` for benchmark instrumentation.

## Database

`db/init/` scripts run automatically when the Postgres container starts fresh.  
To re-seed: `docker compose down -v && docker compose up`.

## Benchmark tooling

- `scripts/run-baseline-experiment.ps1` — PowerShell runner for JMeter matrix experiments
- `scripts/analyze-baseline-experiment.py` — Python analyzer for result CSVs
- `*.jmx` files in the repo root are JMeter test plans
