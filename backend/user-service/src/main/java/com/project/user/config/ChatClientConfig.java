package com.project.user.config;

import io.netty.channel.ChannelOption;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(N8nProperties.class)
public class ChatClientConfig {

    @Bean
    public WebClient n8nWebClient(N8nProperties properties, WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(properties.connectTimeout().toMillis()))
                .responseTimeout(properties.timeout());

        return builder
                .baseUrl(properties.url())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
