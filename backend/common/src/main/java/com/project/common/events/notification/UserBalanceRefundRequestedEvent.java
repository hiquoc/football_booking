package com.project.common.events.notification;

import java.time.Instant;
import java.util.UUID;

public record UserBalanceRefundRequestedEvent(
        UUID userId,
        long amount,
        UUID bookingId,
        String bookingCode,
        String reason,
        Instant occurredAt
) {
}
