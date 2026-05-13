package com.niocess.perflab.client.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RiskProfileRepository extends JpaRepository<RiskProfile, Long> {

    @Query(value = "SELECT * FROM risk_profiles ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<RiskProfile> findRandom();
}
