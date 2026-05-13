package com.niocess.perflab.client.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(value = "SELECT * FROM products ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
    Optional<Product> findRandom();
}
