package com.project.payment.strategy;
import java.math.BigDecimal;
import java.util.UUID;
public record ProviderCheckoutRequest(UUID paymentId, UUID bookingId, int attempt, String bookingCode,
                                      BigDecimal amount, String currency, String customerEmail) {}
