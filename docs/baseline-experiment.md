# Baseline Experiment

This experiment measures end-to-end latency for the current direct path:

```text
JMeter -> client-app -> external-api-simulator -> client-app -> JMeter
```

It is intended as the baseline for a later run where a proxy service sits between
`client-app` and `external-api-simulator`.

## Why This Experiment Is Valid

`client-app` forwards each request to `external-api-simulator` through Spring
`RestClient`. The simulator applies `delayMs` with `setTimeout`, so changing
`delayMs` models slow external I/O without intentionally burning CPU.

The measured JMeter response time is not only simulator delay. It also includes
Spring/Tomcat request handling, HTTP client behavior, Docker networking, host
resource pressure, and queueing. That is acceptable for the baseline because the
future proxy should be evaluated under the same end-to-end conditions.

For the first baseline, keep both services on the same Docker host. Moving the
simulator to another machine is useful later if you need to isolate host resource
contention or include real network latency, but it adds another variable.

## Matrix

- Threads: `100`, `200`, `500`, `1000`, `2000`, `3000`
- External delay: `100`, `250`, `500`, `1000`, `2000`, `3000` ms
- Repeats: `3`
- Loops: `1`
- Requests per thread: `4`
- Cooldown between runs: `15s`

Ramp-up is proportional: `ceil(threads * 0.03)`.

## Run

Start the app stack first:

```powershell
docker compose up --build
```

Preview the matrix without running JMeter:

```powershell
.\scripts\run-baseline-experiment.ps1 -DryRun
```

Run the full baseline:

```powershell
.\scripts\run-baseline-experiment.ps1
```

The runner uses `http://127.0.0.1:8080` and `http://127.0.0.1:8090` by
default to avoid Windows PowerShell `localhost` resolution issues.

Analyze a completed experiment:

```powershell
python .\scripts\analyze-baseline-experiment.py --experiment-dir .\experiments\baseline-YYYYMMDD-HHMMSS
```

Outputs are written under the experiment directory:

- `runs/<run-id>/results.jtl`
- `runs/<run-id>/report/`
- `runs/<run-id>/metadata.json`
- `runs/<run-id>/simulator-metrics.json`
- `summaries/run-summary.csv`
- `summaries/aggregate-summary.csv`
- `graphs/*.svg`

The runner stops remaining repeats for a `(threads, delayMs)` zone after two
consecutive runs exceed stop criteria:

- error rate greater than `10%`
- average response time greater than `60000 ms`
- non-zero JMeter exit code
