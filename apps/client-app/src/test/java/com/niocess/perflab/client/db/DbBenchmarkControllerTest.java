package com.niocess.perflab.client.db;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Optional;

import com.niocess.perflab.client.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DbBenchmarkController.class)
@Import({GlobalExceptionHandler.class, DbDriverConverter.class})
class DbBenchmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private RiskProfileRepository riskProfileRepository;

    // --- /api/db/pricing ---

    @Test
    void pricingDefaultDriverReturns200() throws Exception {
        Product product = mockProduct();
        when(productRepository.findRandom()).thenReturn(Optional.of(product));

        mockMvc.perform(get("/api/db/pricing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    void pricingJdbcDriverReturns200() throws Exception {
        Product product = mockProduct();
        when(productRepository.findRandom()).thenReturn(Optional.of(product));

        mockMvc.perform(get("/api/db/pricing?driver=jdbc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    void pricingPgasyncDriverReturns501() throws Exception {
        mockMvc.perform(get("/api/db/pricing?driver=pgasync"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error").value("pgasync driver not yet implemented"));
    }

    @Test
    void pricingInvalidDriverReturns400() throws Exception {
        mockMvc.perform(get("/api/db/pricing?driver=foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid driver: 'foo'. Valid values: jdbc, pgasync"));
    }

    @Test
    void pricingJdbcDriverReturns200() throws Exception {
        Product product = mockProduct();
        when(productRepository.findRandom()).thenReturn(Optional.of(product));

        mockMvc.perform(get("/api/db/pricing?driver=jdbc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.price").value(9.99))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void pricingPgasyncDriverReturns501() throws Exception {
        mockMvc.perform(get("/api/db/pricing?driver=pgasync"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error").value("pgasync driver not yet implemented"));
    }

    @Test
    void pricingInvalidDriverReturns400() throws Exception {
        mockMvc.perform(get("/api/db/pricing?driver=foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid driver: 'foo'. Valid values: jdbc, pgasync"));
    }

    @Test
    void pricingReturns404WhenTableEmpty() throws Exception {
        when(productRepository.findRandom()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/db/pricing"))
                .andExpect(status().isNotFound());
    }

    // --- /api/db/risk-score ---

    @Test
    void riskScoreDefaultDriverReturns200() throws Exception {
        RiskProfile profile = mockRiskProfile();
        when(riskProfileRepository.findRandom()).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/api/db/risk-score"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("client-42"));
    }

    @Test
    void riskScoreJdbcDriverReturns200() throws Exception {
        RiskProfile profile = mockRiskProfile();
        when(riskProfileRepository.findRandom()).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/api/db/risk-score?driver=jdbc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("client-42"));
    }

    @Test
    void riskScorePgasyncDriverReturns501() throws Exception {
        mockMvc.perform(get("/api/db/risk-score?driver=pgasync"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error").value("pgasync driver not yet implemented"));
    }

    @Test
    void riskScoreInvalidDriverReturns400() throws Exception {
        mockMvc.perform(get("/api/db/risk-score?driver=foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid driver: 'foo'. Valid values: jdbc, pgasync"));
    }

    @Test
    void riskScoreJdbcDriverReturns200() throws Exception {
        RiskProfile profile = mockRiskProfile();
        when(riskProfileRepository.findRandom()).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/api/db/risk-score?driver=jdbc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("client-42"))
                .andExpect(jsonPath("$.score").value(750))
                .andExpect(jsonPath("$.category").value("LOW"));
    }

    @Test
    void riskScorePgasyncDriverReturns501() throws Exception {
        mockMvc.perform(get("/api/db/risk-score?driver=pgasync"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error").value("pgasync driver not yet implemented"));
    }

    @Test
    void riskScoreInvalidDriverReturns400() throws Exception {
        mockMvc.perform(get("/api/db/risk-score?driver=foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid driver: 'foo'. Valid values: jdbc, pgasync"));
    }

    @Test
    void riskScoreReturns404WhenTableEmpty() throws Exception {
        when(riskProfileRepository.findRandom()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/db/risk-score"))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---

    private Product mockProduct() {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(1L);
        when(product.getSku()).thenReturn("SKU-001");
        when(product.getName()).thenReturn("Widget");
        when(product.getPrice()).thenReturn(new BigDecimal("9.99"));
        when(product.getCurrency()).thenReturn("USD");
        return product;
    }

    private RiskProfile mockRiskProfile() {
        RiskProfile profile = mock(RiskProfile.class);
        when(profile.getId()).thenReturn(2L);
        when(profile.getClientId()).thenReturn("client-42");
        when(profile.getScore()).thenReturn(750);
        when(profile.getCategory()).thenReturn("LOW");
        return profile;
    }
}
