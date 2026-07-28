package com.project.payment.dto;
import com.project.payment.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.UUID;
public record PaymentResponse(UUID id, UUID bookingId, PaymentProvider provider, PaymentPurpose purpose, BigDecimal amount,
                              String currency, PaymentStatus status, String failureReason, Instant expiresAt,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {}
