package com.project.booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class UserServiceClientConfig {
    @Bean
    @Qualifier("userServiceRestClient")
    public RestClient userServiceRestClient(
            RestClient.Builder builder,
            @Value("${internal.user-service.url:http://localhost:8081}") String baseUrl,
            @Value("${internal.gateway.secret}") String internalGatewaySecret) {
        return builder
                .baseUrl(baseUrl)
                .defaultHeader(com.project.common.constants.GlobalConstants.HEADER_INTERNAL_SECRET, internalGatewaySecret)
                .build();
    }

    @Bean
    @Qualifier("fieldServiceRestClient")
    public RestClient fieldServiceRestClient(
            RestClient.Builder builder,
            @Value("${internal.field-service.url:http://localhost:8082}") String baseUrl,
            @Value("${internal.gateway.secret}") String internalGatewaySecret) {
        return builder
                .baseUrl(baseUrl)
                .defaultHeader(com.project.common.constants.GlobalConstants.HEADER_INTERNAL_SECRET, internalGatewaySecret)
                .build();
    }
}
