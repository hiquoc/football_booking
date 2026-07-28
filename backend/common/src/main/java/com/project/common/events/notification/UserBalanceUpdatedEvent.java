package com.project.common.events.notification;

import java.time.Instant;
import java.util.UUID;

public record UserBalanceUpdatedEvent(
        UUID userId,
        long balance,
        String reason,
        Instant occurredAt
) {
}
