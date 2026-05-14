package com.niocess.perflab.client.db;

import com.pgasync.Connectible;
import com.pgasync.Row;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class PgAsyncProductRepository {

    private final Connectible pool;

    public PgAsyncProductRepository(Connectible pool) {
        this.pool = pool;
    }

    public CompletableFuture<Optional<Product>> findRandom(double delaySec) {
        return pool.completeQuery(
                "SELECT pg_sleep($1), id, sku, name, price, currency FROM products WHERE id = (SELECT id FROM products ORDER BY random() LIMIT 1)",
                delaySec
        ).thenApply(rs -> {
            var it = rs.iterator();
            if (!it.hasNext()) return Optional.<Product>empty();
            Row row = it.next();
            return Optional.of(new Product(
                    row.getLong("id"),
                    row.getString("sku"),
                    row.getString("name"),
                    row.getBigDecimal("price"),
                    row.getString("currency")
            ));
        });
    }
}
