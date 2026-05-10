package com.niocess.perflab.client;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
}
