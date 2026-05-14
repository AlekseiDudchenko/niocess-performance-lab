package com.niocess.perflab.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BenchmarkController.class)
class BenchmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExternalApiClient externalApiClient;

    @Test
    void proxiesPricingRequest() throws Exception {
        when(externalApiClient.pricing(eq("123"), eq(500)))
                .thenReturn(Map.of("productId", "123", "source", "niocess-external-api-simulator"));

        mockMvc.perform(get("/api/pricing?productId=123&delayMs=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("123"))
                .andExpect(jsonPath("$.source").value("niocess-external-api-simulator"));
    }

    @Test
    void pricingPassesNullDelayMsWhenParamOmitted() throws Exception {
        when(externalApiClient.pricing(eq("123"), isNull()))
                .thenReturn(Map.of("productId", "123"));

        mockMvc.perform(get("/api/pricing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("123"));
    }

    @Test
    void proxiesRiskScoreRequest() throws Exception {
        when(externalApiClient.riskScore(eq("456"), eq(500)))
                .thenReturn(Map.of("userId", "456", "source", "niocess-external-api-simulator"));

        mockMvc.perform(get("/api/risk-score?userId=456&delayMs=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("456"))
                .andExpect(jsonPath("$.source").value("niocess-external-api-simulator"));
    }

    @Test
    void proxiesExternalReportRequest() throws Exception {
        when(externalApiClient.externalReport(eq("tenant1"), eq("7d"), eq(200)))
                .thenReturn(Map.of("tenantId", "tenant1", "period", "7d"));

        mockMvc.perform(get("/api/analytics/external-report?tenantId=tenant1&period=7d&delayMs=200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant1"))
                .andExpect(jsonPath("$.period").value("7d"));
    }

    @Test
    void proxiesAiSummaryRequest() throws Exception {
        when(externalApiClient.aiSummary(any(), eq(100)))
                .thenReturn(Map.of("summary", "ok"));

        mockMvc.perform(post("/api/ai/summary?delayMs=100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("ok"));
    }
}
