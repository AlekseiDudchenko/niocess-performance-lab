import { createServer } from "node:http";
import { randomInt } from "node:crypto";
import { fileURLToPath } from "node:url";

const config = {
  port: numberFromEnv("PORT", 8090),
  fixedDelayMs: numberFromEnv("EXTERNAL_API_DELAY_MS", 0),
  minDelayMs: numberFromEnv("EXTERNAL_API_MIN_DELAY_MS", null),
  maxDelayMs: numberFromEnv("EXTERNAL_API_MAX_DELAY_MS", null),
};

const metrics = {
  startedAt: new Date().toISOString(),
  totalRequests: 0,
  totalDelayMs: 0,
  activeRequests: 0,
  maxConcurrentRequests: 0,
  errorCount: 0,
  perEndpoint: {},
};

export function createApp() {
  return createServer(async (req, res) => {
    const started = Date.now();
    const url = new URL(req.url ?? "/", "http://localhost");
    const route = routeKey(req.method, url.pathname);

    metrics.totalRequests += 1;
    metrics.activeRequests += 1;
    metrics.maxConcurrentRequests = Math.max(metrics.maxConcurrentRequests, metrics.activeRequests);
    metrics.perEndpoint[route] = (metrics.perEndpoint[route] ?? 0) + 1;

    try {
      await delay(resolveDelayMs(url));

      if (req.method === "GET" && url.pathname === "/health") {
        sendJson(res, 200, { status: "ok", service: "niocess-external-api-simulator" });
        return;
      }

      if (req.method === "GET" && url.pathname === "/metrics") {
        sendJson(res, 200, {
          ...metrics,
          averageResponseDelayMs: metrics.totalRequests === 0
            ? 0
            : Math.round(metrics.totalDelayMs / metrics.totalRequests),
        });
        return;
      }

      if (req.method === "POST" && url.pathname === "/metrics/reset") {
        resetMetrics();
        sendJson(res, 200, { status: "reset" });
        return;
      }

      if (req.method === "GET" && url.pathname === "/external/pricing") {
        const productId = url.searchParams.get("productId") ?? "unknown";
        sendJson(res, 200, {
          productId,
          price: priceFor(productId),
          currency: "EUR",
          source: "niocess-external-api-simulator",
          generatedAt: new Date().toISOString(),
        });
        return;
      }

      if (req.method === "GET" && url.pathname === "/external/risk-score") {
        const userId = url.searchParams.get("userId") ?? "unknown";
        const riskScore = scoreFor(userId);
        sendJson(res, 200, {
          userId,
          riskScore,
          category: riskScore < 0.35 ? "low" : riskScore < 0.7 ? "medium" : "high",
          source: "niocess-external-api-simulator",
        });
        return;
      }

      if (req.method === "GET" && url.pathname === "/external/analytics/report") {
        const tenantId = url.searchParams.get("tenantId") ?? "unknown";
        const period = url.searchParams.get("period") ?? "30d";
        sendJson(res, 200, {
          tenantId,
          period,
          revenue: 120000 + stableNumber(tenantId, 50000),
          orders: 3500 + stableNumber(period + tenantId, 2500),
          activeUsers: 700 + stableNumber(tenantId + period, 400),
          source: "niocess-external-api-simulator",
        });
        return;
      }

      if (req.method === "POST" && url.pathname === "/external/ai/summary") {
        const body = await readJsonBody(req);
        const text = typeof body.text === "string" ? body.text : "";
        sendJson(res, 200, {
          summary: summarize(text),
          model: "simulated-ai-api",
          source: "niocess-external-api-simulator",
        });
        return;
      }

      sendJson(res, 404, { error: "not_found" });
    } catch (error) {
      metrics.errorCount += 1;
      sendJson(res, 500, {
        error: "internal_error",
        message: error instanceof Error ? error.message : "Unknown error",
      });
    } finally {
      metrics.activeRequests -= 1;
      metrics.totalDelayMs += Date.now() - started;
    }
  });
}

function routeKey(method, pathname) {
  return `${method ?? "GET"} ${pathname}`;
}

function resolveDelayMs(url) {
  const queryDelay = Number(url.searchParams.get("delayMs"));
  if (Number.isFinite(queryDelay) && queryDelay >= 0) {
    return queryDelay;
  }

  if (config.minDelayMs !== null && config.maxDelayMs !== null) {
    const min = Math.min(config.minDelayMs, config.maxDelayMs);
    const max = Math.max(config.minDelayMs, config.maxDelayMs);
    return randomInt(min, max + 1);
  }

  return config.fixedDelayMs;
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function sendJson(res, statusCode, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(statusCode, {
    "content-type": "application/json; charset=utf-8",
    "content-length": Buffer.byteLength(body),
  });
  res.end(body);
}

async function readJsonBody(req) {
  const chunks = [];
  for await (const chunk of req) {
    chunks.push(chunk);
  }

  if (chunks.length === 0) {
    return {};
  }

  return JSON.parse(Buffer.concat(chunks).toString("utf8"));
}

function resetMetrics() {
  metrics.startedAt = new Date().toISOString();
  metrics.totalRequests = 0;
  metrics.totalDelayMs = 0;
  metrics.activeRequests = 0;
  metrics.maxConcurrentRequests = 0;
  metrics.errorCount = 0;
  metrics.perEndpoint = {};
}

function numberFromEnv(name, fallback) {
  const raw = process.env[name];
  if (raw === undefined || raw === "") {
    return fallback;
  }
  const parsed = Number(raw);
  return Number.isFinite(parsed) ? parsed : fallback;
}

function stableNumber(input, modulo) {
  let hash = 0;
  for (const char of String(input)) {
    hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  }
  return hash % modulo;
}

function priceFor(productId) {
  return Number((9.99 + stableNumber(productId, 9000) / 100).toFixed(2));
}

function scoreFor(userId) {
  return Number((stableNumber(userId, 100) / 100).toFixed(2));
}

function summarize(text) {
  if (!text.trim()) {
    return "Generated summary for empty test input.";
  }
  return `Generated summary for ${text.trim().split(/\s+/).length} words of test input.`;
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  createApp().listen(config.port, () => {
    console.log(`niocess-external-api-simulator listening on :${config.port}`);
  });
}
