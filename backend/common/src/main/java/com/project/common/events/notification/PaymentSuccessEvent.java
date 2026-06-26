package com.project.common.events.notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentSuccessEvent(
        UUID paymentId,
        UUID bookingId,
        String bookingCode,
        UUID userId,
        String userEmail,
        BigDecimal amount,
        Instant occurredAt
) {
}
