package com.niocess.perflab.client.db;

import com.pgasync.Connectible;
import com.pgasync.Row;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class PgAsyncRiskProfileRepository {

    private final Connectible pool;

    public PgAsyncRiskProfileRepository(Connectible pool) {
        this.pool = pool;
    }

    public CompletableFuture<Optional<RiskProfile>> findRandom() {
        return pool.completeQuery(
                "SELECT id, client_id, score, category FROM risk_profiles ORDER BY random() LIMIT 1"
        ).thenApply(rs -> {
            var it = rs.iterator();
            if (!it.hasNext()) return Optional.<RiskProfile>empty();
            Row row = it.next();
            return Optional.of(new RiskProfile(
                    row.getLong("id"),
                    row.getString("client_id"),
                    row.getInt("score"),
                    row.getString("category")
            ));
        });
    }
}
