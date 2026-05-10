package com.niocess.perflab.client;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ExternalApiClient {

    private final RestClient restClient;

    public ExternalApiClient(RestClient externalApiRestClient) {
        this.restClient = externalApiRestClient;
    }

    public Map<String, Object> pricing(String productId, Integer delayMs) {
        return get("/external/pricing?productId={productId}&delayMs={delayMs}", productId, valueOrZero(delayMs));
    }

    public Map<String, Object> riskScore(String userId, Integer delayMs) {
        return get("/external/risk-score?userId={userId}&delayMs={delayMs}", userId, valueOrZero(delayMs));
    }

    public Map<String, Object> externalReport(String tenantId, String period, Integer delayMs) {
        return get(
                "/external/analytics/report?tenantId={tenantId}&period={period}&delayMs={delayMs}",
                tenantId,
                period,
                valueOrZero(delayMs));
    }

    public Map<String, Object> aiSummary(Map<String, Object> request, Integer delayMs) {
        return restClient.post()
                .uri("/external/ai/summary?delayMs={delayMs}", valueOrZero(delayMs))
                .body(request)
                .retrieve()
                .body(Map.class);
    }

    private Map<String, Object> get(String uri, Object... variables) {
        return restClient.get()
                .uri(uri, variables)
                .retrieve()
                .body(Map.class);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
