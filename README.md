# Performance Lab

A benchmark lab for measuring latency and throughput across different backend strategies. The goal is to run controlled experiments, compare results, and document what actually changes under load.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Load Test                            │
│                    (JMeter / k6)                            │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP
                         ▼
┌────────────────────────────────────────────────────────────┐
│                     client-app                             │
│                 Spring Boot  :8080                         │
│                                                            │
│  /api/*           → ExternalApiClient (RestClient)         │
│  /api/db/*        → DbBenchmarkController                  │
│    ?driver=jdbc   → ProductRepository (Spring Data JPA)    │
│    ?driver=pgasync→ PgAsyncProductRepository (Netty)       │
└────────┬──────────────────────────┬───────────────────────-┘
         │ HTTP                     │ TCP
         ▼                          ▼
┌────────────────────┐   ┌──────────────────────────────────┐
│ external-api-      │   │          PostgreSQL               │
│ simulator  :8090   │   │            :5432                  │
│ (slow/paid API)    │   │  products (500 rows)              │
│                    │   │  risk_profiles (500 rows)         │
└────────────────────┘   └──────────────────────────────────┘
```

### Data flow — blocking JDBC path

```
JMeter → client-app (Tomcat thread held) → HikariCP → PostgreSQL
                                ↑
                   thread blocked until query returns
```

### Data flow — non-blocking pgasync path

```
JMeter → client-app (Tomcat thread released) → Netty event loop → PostgreSQL
                                ↑
               CompletableFuture returned immediately;
               Tomcat thread free to serve other requests
```

## Stack

| Service | Tech | Port |
|---------|------|------|
| `client-app` | Spring Boot 3, Java 21, Gradle | 8080 |
| `external-api-simulator` | Node.js, ESM, zero deps | 8090 |
| `postgres` | PostgreSQL 16 | 5432 |

## Quick Start

```bash
docker compose up --build
```

Verify the stack is up:

```bash
curl http://localhost:8080/api/db/pricing          # JDBC (default)
curl http://localhost:8080/api/db/pricing?driver=pgasync  # pgasync
curl http://localhost:8090/metrics                 # simulator stats
```

---

## Scenario: JDBC vs pgasync (current)

### What it tests

Under high concurrency, JDBC holds a Tomcat thread for the full duration of each DB query. When threads run out, new requests queue. pgasync releases the Tomcat thread immediately and uses Netty to drive the query asynchronously — the thread is free to serve another request while the query runs.

This scenario measures how much that difference matters at the database layer with 500-row tables and a random-row query on each request.

### Endpoints

| Endpoint | Driver | Behavior |
|----------|--------|----------|
| `GET /api/db/pricing?driver=jdbc` | Spring Data JPA / Hibernate | Tomcat thread held |
| `GET /api/db/pricing?driver=pgasync` | postgres-async-driver (Netty) | Tomcat thread released |
| `GET /api/db/risk-score?driver=jdbc` | Spring Data JPA / Hibernate | Tomcat thread held |
| `GET /api/db/risk-score?driver=pgasync` | postgres-async-driver (Netty) | Tomcat thread released |

### JMeter test plan

Test plan: `db-driver-benchmark.jmx`

Two thread groups run in parallel — one hitting `?driver=jdbc`, one hitting `?driver=pgasync`. Each group hammers both `/api/db/pricing` and `/api/db/risk-score` in a loop for the full duration.

Parameters (all optional, defaults shown):

| Parameter | Default | Description |
|-----------|---------|-------------|
| `threads` | `100` | Concurrent users per thread group |
| `rampup` | `10` | Ramp-up period in seconds |
| `duration` | `30` | Steady-state duration in seconds |

Run:

```bash
jmeter -n -t db-driver-benchmark.jmx \
  -Jthreads=300 -Jrampup=10 -Jduration=30 \
  -l results.jtl \
  -e -o report/
```

Open `report/index.html` for the HTML dashboard, or inspect `results.jtl` directly.

### Results

#### 100 threads per driver, 30 s

| Metric | JDBC | pgasync | Delta |
|--------|------|---------|-------|
| Throughput (TPS) | 7,563 | 7,977 | **+5.5%** |
| Avg latency (ms) | 11.0 | 10.4 | -5.5% |
| p95 (ms) | 29 | 18 | **-38%** |
| p99 (ms) | 46 | 26 | **-43%** |

At 100 threads the Tomcat pool is not yet saturated, so the throughput gain is modest. The tail-latency improvement is already significant.

#### 300 threads per driver, 30 s

| Metric | JDBC | pgasync | Delta |
|--------|------|---------|-------|
| Throughput (TPS) | 7,594 | 9,312 | **+22.6%** |
| Avg latency (ms) | 29.7 | 24.2 | -18.5% |
| p95 (ms) | 100 | 42 | **-58%** |
| p99 (ms) | 159 | 59 | **-63%** |
| Max (ms) | 488 | 158 | -68% |

At 300 threads the Tomcat pool becomes the bottleneck for JDBC. pgasync bypasses it — throughput jumps 22% and tail latency drops by more than half.

---

## How to Add a New Scenario

Follow these steps to add a new benchmark dimension (e.g. caching layer, connection pool sizes, a different query type).

### Step 1 — Add the endpoint(s)

Add a controller or extend `DbBenchmarkController`. Use `CompletableFuture<ResponseEntity<T>>` as the return type so Spring MVC releases the Tomcat thread while async work runs.

For a blocking implementation, wrap synchronously:

```java
return CompletableFuture.completedFuture(
    repository.findSomething()
        .<ResponseEntity<T>>map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build()));
```

For a non-blocking implementation, chain on the future:

```java
return asyncRepository.findSomething()
    .thenApply(opt -> opt.<ResponseEntity<T>>map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build()));
```

### Step 2 — Add tests

`DbBenchmarkControllerTest` uses `@WebMvcTest` + `@MockitoBean`. Add one test per behaviour:

```java
@Test
void myEndpointReturns200() throws Exception {
    MyEntity entity = new MyEntity(...);   // create before when() to avoid Mockito issues
    when(myRepository.findSomething())
        .thenReturn(CompletableFuture.completedFuture(Optional.of(entity)));

    MvcResult result = mockMvc.perform(get("/api/db/my-endpoint"))
        .andExpect(request().asyncStarted()).andReturn();
    mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
}
```

Verify everything passes:

```bash
cd apps/client-app && ./gradlew test
```

### Step 3 — Create a JMeter test plan

Copy `db-driver-benchmark.jmx` as a starting point. The plan structure is:

```
TestPlan
└── ThreadGroup (variant A)
│   ├── HTTP Defaults (localhost:8080)
│   ├── HTTPSampler → /api/db/your-endpoint?variant=a
│   └── ResponseAssertion (assert 200)
└── ThreadGroup (variant B)
    ├── HTTP Defaults (localhost:8080)
    ├── HTTPSampler → /api/db/your-endpoint?variant=b
    └── ResponseAssertion (assert 200)
```

Parameterize thread count, ramp-up, and duration using `${__P(threads,100)}` etc. so the same plan works at different load levels without editing.

### Step 4 — Run and record results

```bash
# Start the stack
docker compose up --build

# Warm-up run (results discarded)
jmeter -n -t your-scenario.jmx -Jthreads=50 -Jduration=15 -l /dev/null

# Measured runs
jmeter -n -t your-scenario.jmx -Jthreads=100 -Jduration=30 -l results-100t.jtl
jmeter -n -t your-scenario.jmx -Jthreads=300 -Jduration=30 -l results-300t.jtl
```

### Step 5 — Document results

Add a results table to this README (or to `docs/`) with throughput, avg, p95, p99, and max per variant at each thread level. Include the delta so the improvement (or regression) is visible at a glance.

---

## Project Structure

```
apps/
  client-app/               Spring Boot REST API
    src/main/java/
      .../client/
        BenchmarkController.java      → proxies to external-api-simulator
        DbBenchmarkController.java    → queries PostgreSQL (JDBC or pgasync)
        db/
          Product.java / RiskProfile.java          entities
          ProductRepository.java / RiskProfileRepository.java  (JPA)
          PgAsyncProductRepository.java / PgAsyncRiskProfileRepository.java
          PgAsyncConfig.java           Netty pool configuration
          DbDriver.java / DbDriverConverter.java   ?driver= param handling
    libs/
      postgres-async-driver-1.0.5.jar  vendored from AlekseiDudchenko fork
  external-api-simulator/   Node.js slow-API simulator

db/
  init/
    01-schema.sql            creates products + risk_profiles tables
    02-seed.sql              inserts 500 rows into each table

scripts/
  run-baseline-experiment.ps1    PowerShell JMeter matrix runner
  analyze-baseline-experiment.py Python CSV analyzer

*.jmx                        JMeter test plans
docker-compose.yml
```

## Configuration

All environment variables have defaults that work out of the box with Docker Compose.

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://postgres:5432/perflab` | PostgreSQL JDBC URL |
| `DB_USER` | `perflab` | Database username |
| `DB_PASSWORD` | `perflab` | Database password |
| `POSTGRES_DB` | `perflab` | PostgreSQL database name |
| `POSTGRES_USER` | `perflab` | PostgreSQL user |
| `POSTGRES_PASSWORD` | `perflab` | PostgreSQL password |
| `EXTERNAL_API_BASE_URL` | `http://external-api-simulator:8090` | Simulator base URL |

Override in a `.env` file next to `docker-compose.yml`.

## Development

Build and test `client-app`:

```bash
cd apps/client-app
./gradlew build          # compile + all tests
./gradlew test           # tests only
./gradlew -x test bootJar  # build jar without tests (used by Dockerfile)
```

Run a specific test class:

```bash
./gradlew test --tests "com.niocess.perflab.client.db.DbBenchmarkControllerTest"
```

Run `external-api-simulator` tests:

```bash
cd apps/external-api-simulator
node --test
```

Re-seed the database (wipes all data):

```bash
docker compose down -v && docker compose up
```
