import assert from "node:assert/strict";
import { test } from "node:test";
import { createApp } from "../src/server.js";

test("serves pricing responses and metrics", async () => {
  const app = createApp();
  const baseUrl = await listen(app);

  const pricing = await getJson(`${baseUrl}/external/pricing?productId=123`);
  assert.equal(pricing.productId, "123");
  assert.equal(pricing.currency, "EUR");
  assert.equal(pricing.source, "niocess-external-api-simulator");

  const metrics = await getJson(`${baseUrl}/metrics`);
  assert.equal(metrics.totalRequests, 2);
  assert.equal(metrics.perEndpoint["GET /external/pricing"], 1);
  assert.equal(metrics.perEndpoint["GET /metrics"], 1);

  await close(app);
});

test("serves AI summary", async () => {
  const app = createApp();
  const baseUrl = await listen(app);

  const response = await fetch(`${baseUrl}/external/ai/summary`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ text: "Niocess reduces duplicate external API calls." }),
  });
  const payload = await response.json();

  assert.equal(response.status, 200);
  assert.equal(payload.model, "simulated-ai-api");
  assert.match(payload.summary, /Generated summary/);

  await close(app);
});

function listen(app) {
  return new Promise((resolve) => {
    app.listen(0, "127.0.0.1", () => {
      const address = app.address();
      resolve(`http://127.0.0.1:${address.port}`);
    });
  });
}

function close(app) {
  return new Promise((resolve, reject) => {
    app.close((error) => (error ? reject(error) : resolve()));
  });
}

async function getJson(url) {
  const response = await fetch(url);
  assert.equal(response.status, 200);
  return response.json();
}
