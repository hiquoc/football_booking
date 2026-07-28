package com.project.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record UserBalanceUpdateMessage(
        UUID userId,
        long balance,
        String reason,
        Instant occurredAt
) {
}
