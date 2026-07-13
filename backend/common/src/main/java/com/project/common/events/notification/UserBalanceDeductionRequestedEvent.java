package com.project.common.events.notification;

import java.time.Instant;
import java.util.UUID;

public record UserBalanceDeductionRequestedEvent(
        UUID userId,
        long amount,
        UUID bookingId,
        String bookingCode,
        String reason,
        Instant occurredAt
) {
}
