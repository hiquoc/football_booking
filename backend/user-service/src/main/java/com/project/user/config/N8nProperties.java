package com.project.user.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "chat.n8n")
public record N8nProperties(
        @NotBlank String url,
        @NotBlank String key,
        Duration timeout,
        Duration connectTimeout,
        @Min(0) int maxRetries
) {
    public N8nProperties {
        timeout = timeout == null ? Duration.ofSeconds(20) : timeout;
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
    }
}
