package com.niocess.perflab.client.db;

import com.github.pgasync.netty.NettyConnectibleBuilder;
import com.pgasync.Connectible;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class PgAsyncConfig {

    @Value("${spring.datasource.url}") private String jdbcUrl;
    @Value("${spring.datasource.username}") private String username;
    @Value("${spring.datasource.password}") private String password;

    @Bean(destroyMethod = "close")
    public Connectible pgAsyncPool() {
        // jdbc:postgresql://host:port/database → strip "jdbc:" prefix for URI parsing
        URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String database = uri.getPath().substring(1); // strip leading "/"

        return new NettyConnectibleBuilder()
                .hostname(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                .pool();
    }
}
