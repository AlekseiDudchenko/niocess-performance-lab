package com.niocess.perflab.client.db;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.niocess.perflab.client.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(DbBenchmarkController.class)
@Import({GlobalExceptionHandler.class, DbDriverConverter.class})
class DbBenchmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private RiskProfileRepository riskProfileRepository;

    @MockitoBean
    private PgAsyncProductRepository pgAsyncProductRepository;

    @MockitoBean
    private PgAsyncRiskProfileRepository pgAsyncRiskProfileRepository;

    // --- /api/db/pricing ---

    @Test
    void pricingDefaultDriverReturns200() throws Exception {
        Product product = mockProduct();
        when(productRepository.findRandom(anyDouble())).thenReturn(Optional.of(product));

        MvcResult result = mockMvc.perform(get("/api/db/pricing"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.price").value(9.99))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void pricingJdbcDriverReturns200() throws Exception {
        Product product = mockProduct();
        when(productRepository.findRandom(anyDouble())).thenReturn(Optional.of(product));

        MvcResult result = mockMvc.perform(get("/api/db/pricing?driver=jdbc"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.price").value(9.99))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void pricingPgasyncDriverReturns200() throws Exception {
        Product product = mockProduct();
        when(pgAsyncProductRepository.findRandom(anyDouble()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(product)));

        MvcResult result = mockMvc.perform(get("/api/db/pricing?driver=pgasync"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.name").value("Widget"))
                .andExpect(jsonPath("$.price").value(9.99))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void pricingJdbcDelayMs100PassesCorrectDelay() throws Exception {
        Product product = mockProduct();
        when(productRepository.findRandom(eq(0.1))).thenReturn(Optional.of(product));

        MvcResult result = mockMvc.perform(get("/api/db/pricing?driver=jdbc&delayMs=100"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    void pricingPgasyncDelayMs100PassesCorrectDelay() throws Exception {
        Product product = mockProduct();
        when(pgAsyncProductRepository.findRandom(eq(0.1)))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(product)));

        MvcResult result = mockMvc.perform(get("/api/db/pricing?driver=pgasync&delayMs=100"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    void pricingInvalidDriverReturns400() throws Exception {
        mockMvc.perform(get("/api/db/pricing?driver=foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid driver: 'foo'. Valid values: jdbc, pgasync"));
    }

    @Test
    void pricingReturns404WhenTableEmpty() throws Exception {
        when(productRepository.findRandom(anyDouble())).thenReturn(Optional.empty());

        MvcResult result = mockMvc.perform(get("/api/db/pricing"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isNotFound());
    }

    // --- /api/db/risk-score ---

    @Test
    void riskScoreDefaultDriverReturns200() throws Exception {
        RiskProfile profile = mockRiskProfile();
        when(riskProfileRepository.findRandom(anyDouble())).thenReturn(Optional.of(profile));

        MvcResult result = mockMvc.perform(get("/api/db/risk-score"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("client-42"))
                .andExpect(jsonPath("$.score").value(750))
                .andExpect(jsonPath("$.category").value("LOW"));
    }

    @Test
    void riskScoreJdbcDriverReturns200() throws Exception {
        RiskProfile profile = mockRiskProfile();
        when(riskProfileRepository.findRandom(anyDouble())).thenReturn(Optional.of(profile));

        MvcResult result = mockMvc.perform(get("/api/db/risk-score?driver=jdbc"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("client-42"))
                .andExpect(jsonPath("$.score").value(750))
                .andExpect(jsonPath("$.category").value("LOW"));
    }

    @Test
    void riskScorePgasyncDriverReturns200() throws Exception {
        RiskProfile profile = mockRiskProfile();
        when(pgAsyncRiskProfileRepository.findRandom(anyDouble()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(profile)));

        MvcResult result = mockMvc.perform(get("/api/db/risk-score?driver=pgasync"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("client-42"))
                .andExpect(jsonPath("$.score").value(750))
                .andExpect(jsonPath("$.category").value("LOW"));
    }

    @Test
    void riskScoreJdbcDelayMs100PassesCorrectDelay() throws Exception {
        RiskProfile profile = mockRiskProfile();
        when(riskProfileRepository.findRandom(eq(0.1))).thenReturn(Optional.of(profile));

        MvcResult result = mockMvc.perform(get("/api/db/risk-score?driver=jdbc&delayMs=100"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("client-42"));
    }

    @Test
    void riskScorePgasyncDelayMs100PassesCorrectDelay() throws Exception {
        RiskProfile profile = mockRiskProfile();
        when(pgAsyncRiskProfileRepository.findRandom(eq(0.1)))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(profile)));

        MvcResult result = mockMvc.perform(get("/api/db/risk-score?driver=pgasync&delayMs=100"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("client-42"));
    }

    @Test
    void riskScoreInvalidDriverReturns400() throws Exception {
        mockMvc.perform(get("/api/db/risk-score?driver=foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid driver: 'foo'. Valid values: jdbc, pgasync"));
    }

    @Test
    void riskScoreReturns404WhenTableEmpty() throws Exception {
        when(riskProfileRepository.findRandom(anyDouble())).thenReturn(Optional.empty());

        MvcResult result = mockMvc.perform(get("/api/db/risk-score"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
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
