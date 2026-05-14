package com.niocess.perflab.client.db;

import com.pgasync.Connectible;
import com.pgasync.Row;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PgAsyncProductRepository {

    private final Connectible pool;

    public PgAsyncProductRepository(Connectible pool) {
        this.pool = pool;
    }

    public Optional<Product> findRandom() {
        return pool.completeQuery(
                "SELECT id, sku, name, price, currency FROM products ORDER BY random() LIMIT 1"
        ).thenApply(rs -> {
            var it = rs.iterator();
            if (!it.hasNext()) return Optional.<Product>empty();
            Row row = it.next();
            Product p = new Product();
            p.setId(row.getLong("id"));
            p.setSku(row.getString("sku"));
            p.setName(row.getString("name"));
            p.setPrice(row.getBigDecimal("price"));
            p.setCurrency(row.getString("currency"));
            return Optional.of(p);
        }).join();
    }
}
