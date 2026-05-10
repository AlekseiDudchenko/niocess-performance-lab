package com.niocess.perflab.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@EnableConfigurationProperties(ExternalApiProperties.class)
public class ClientAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientAppApplication.class, args);
    }

    @Bean
    RestClient externalApiRestClient(ExternalApiProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }
}

@ConfigurationProperties(prefix = "external-api")
record ExternalApiProperties(String baseUrl) {
}
