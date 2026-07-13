package com.project.payment.strategy;
import java.time.Instant;
public record ProviderCheckoutResult(String sessionId, String checkoutUrl, Instant expiresAt) {}
