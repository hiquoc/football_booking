package com.project.payment.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
@Validated @ConfigurationProperties(prefix="payment.stripe")
public record StripeProperties(@NotBlank String secretKey, @NotBlank String webhookSecret, @NotBlank String frontendUrl) {}
