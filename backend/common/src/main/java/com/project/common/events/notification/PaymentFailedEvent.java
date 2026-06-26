package com.project.common.events.notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID paymentId,
        UUID bookingId,
        String bookingCode,
        UUID userId,
        String userEmail,
        BigDecimal amount,
        String reason,
        Instant occurredAt
) {
}
