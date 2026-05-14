package com.niocess.perflab.client.db;

import com.github.pgasync.netty.NettyConnectibleBuilder;
import com.pgasync.Connectible;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PgAsyncConfig {

    @Value("${pgasync.host:localhost}") private String host;
    @Value("${pgasync.port:5432}") private int port;
    @Value("${pgasync.database:perflab}") private String database;
    @Value("${pgasync.username:perflab}") private String username;
    @Value("${pgasync.password:perflab}") private String password;

    private Connectible pool;

    @Bean
    public Connectible pgAsyncPool() {
        pool = new NettyConnectibleBuilder()
                .hostname(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                .pool();
        return pool;
    }

    @PreDestroy
    public void closePool() {
        if (pool != null) {
            pool.close().join();
        }
    }
}
