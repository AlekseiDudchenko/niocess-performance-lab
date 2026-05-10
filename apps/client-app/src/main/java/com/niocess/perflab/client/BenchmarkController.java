package com.niocess.perflab.client;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BenchmarkController {

    private final ExternalApiClient externalApiClient;

    public BenchmarkController(ExternalApiClient externalApiClient) {
        this.externalApiClient = externalApiClient;
    }

    @GetMapping("/api/pricing")
    Map<String, Object> pricing(
            @RequestParam(defaultValue = "123") String productId,
            @RequestParam(required = false) Integer delayMs) {
        return externalApiClient.pricing(productId, delayMs);
    }

    @GetMapping("/api/risk-score")
    Map<String, Object> riskScore(
            @RequestParam(defaultValue = "456") String userId,
            @RequestParam(required = false) Integer delayMs) {
        return externalApiClient.riskScore(userId, delayMs);
    }

    @GetMapping("/api/analytics/external-report")
    Map<String, Object> externalReport(
            @RequestParam(defaultValue = "123") String tenantId,
            @RequestParam(defaultValue = "30d") String period,
            @RequestParam(required = false) Integer delayMs) {
        return externalApiClient.externalReport(tenantId, period, delayMs);
    }

    @PostMapping("/api/ai/summary")
    Map<String, Object> aiSummary(
            @RequestBody Map<String, Object> request,
            @RequestParam(required = false) Integer delayMs) {
        return externalApiClient.aiSummary(request, delayMs);
    }
}
