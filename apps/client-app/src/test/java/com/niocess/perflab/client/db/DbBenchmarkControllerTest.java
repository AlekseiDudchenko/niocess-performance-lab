package com.niocess.perflab.client.db;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DbBenchmarkController.class)
class DbBenchmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private RiskProfileRepository riskProfileRepository;

    @Test
    void pricingReturns200WithProduct() throws Exception {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(1L);
        when(product.getSku()).thenReturn("SKU-001");
        when(product.getName()).thenReturn("Widget");
        when(product.getPrice()).thenReturn(new BigDecimal("9.99"));
        when(product.getCurrency()).thenReturn("USD");
        when(productRepository.findRandom()).thenReturn(Optional.of(product));

        mockMvc.perform(get("/api/db/pricing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.price").value(9.99))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void pricingReturns404WhenTableEmpty() throws Exception {
        when(productRepository.findRandom()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/db/pricing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void riskScoreReturns200WithProfile() throws Exception {
        RiskProfile profile = mock(RiskProfile.class);
        when(profile.getId()).thenReturn(2L);
        when(profile.getClientId()).thenReturn("client-42");
        when(profile.getScore()).thenReturn(750);
        when(profile.getCategory()).thenReturn("LOW");
        when(riskProfileRepository.findRandom()).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/api/db/risk-score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("client-42"))
                .andExpect(jsonPath("$.score").value(750))
                .andExpect(jsonPath("$.category").value("LOW"));
    }

    @Test
    void riskScoreReturns404WhenTableEmpty() throws Exception {
        when(riskProfileRepository.findRandom()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/db/risk-score"))
                .andExpect(status().isNotFound());
    }
}
