package com.niocess.perflab.client.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/db")
public class DbBenchmarkController {

    private static final Logger log = LoggerFactory.getLogger(DbBenchmarkController.class);

    private final ProductRepository productRepository;
    private final RiskProfileRepository riskProfileRepository;
    private final PgAsyncProductRepository pgAsyncProductRepository;
    private final PgAsyncRiskProfileRepository pgAsyncRiskProfileRepository;

    public DbBenchmarkController(ProductRepository productRepository,
                                  RiskProfileRepository riskProfileRepository,
                                  PgAsyncProductRepository pgAsyncProductRepository,
                                  PgAsyncRiskProfileRepository pgAsyncRiskProfileRepository) {
        this.productRepository = productRepository;
        this.riskProfileRepository = riskProfileRepository;
        this.pgAsyncProductRepository = pgAsyncProductRepository;
        this.pgAsyncRiskProfileRepository = pgAsyncRiskProfileRepository;
    }

    @GetMapping("/pricing")
    public CompletableFuture<ResponseEntity<Product>> pricing(
            @RequestParam(defaultValue = "jdbc") DbDriver driver) {
        log.debug("db/pricing driver={}", driver);
        if (driver == DbDriver.PGASYNC) {
            return pgAsyncProductRepository.findRandom()
                    .thenApply(opt -> opt.<ResponseEntity<Product>>map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build()));
        }
        return CompletableFuture.completedFuture(
                productRepository.findRandom()
                        .<ResponseEntity<Product>>map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build()));
    }

    @GetMapping("/risk-score")
    public CompletableFuture<ResponseEntity<RiskProfile>> riskScore(
            @RequestParam(defaultValue = "jdbc") DbDriver driver) {
        log.debug("db/risk-score driver={}", driver);
        if (driver == DbDriver.PGASYNC) {
            return pgAsyncRiskProfileRepository.findRandom()
                    .thenApply(opt -> opt.<ResponseEntity<RiskProfile>>map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build()));
        }
        return CompletableFuture.completedFuture(
                riskProfileRepository.findRandom()
                        .<ResponseEntity<RiskProfile>>map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build()));
    }
}
