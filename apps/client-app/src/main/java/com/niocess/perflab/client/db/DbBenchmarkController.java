package com.niocess.perflab.client.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/db")
public class DbBenchmarkController {

    private static final Logger log = LoggerFactory.getLogger(DbBenchmarkController.class);

    private final ProductRepository productRepository;
    private final RiskProfileRepository riskProfileRepository;

    public DbBenchmarkController(ProductRepository productRepository,
                                  RiskProfileRepository riskProfileRepository) {
        this.productRepository = productRepository;
        this.riskProfileRepository = riskProfileRepository;
    }

    @GetMapping("/pricing")
    public ResponseEntity<Product> pricing(@RequestParam(defaultValue = "jdbc") DbDriver driver) {
        log.info("db/pricing driver={}", driver);
        if (driver == DbDriver.PGASYNC) {
            throw new UnsupportedOperationException("pgasync driver not yet implemented");
        }
        return productRepository.findRandom()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/risk-score")
    public ResponseEntity<RiskProfile> riskScore(@RequestParam(defaultValue = "jdbc") DbDriver driver) {
        log.info("db/risk-score driver={}", driver);
        if (driver == DbDriver.PGASYNC) {
            throw new UnsupportedOperationException("pgasync driver not yet implemented");
        }
        return riskProfileRepository.findRandom()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
