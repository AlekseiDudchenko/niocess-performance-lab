package com.niocess.perflab.client.db;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/db")
public class DbBenchmarkController {

    private final ProductRepository productRepository;
    private final RiskProfileRepository riskProfileRepository;

    public DbBenchmarkController(ProductRepository productRepository,
                                  RiskProfileRepository riskProfileRepository) {
        this.productRepository = productRepository;
        this.riskProfileRepository = riskProfileRepository;
    }

    @GetMapping("/pricing")
    public ResponseEntity<Product> pricing() {
        return productRepository.findRandom()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/risk-score")
    public ResponseEntity<RiskProfile> riskScore() {
        return riskProfileRepository.findRandom()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
