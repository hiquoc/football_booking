package com.project.common.events.notification;

import java.time.Instant;
import java.util.UUID;

public record PlatformBanClearedEvent(
        UUID userId,
        UUID clearedBy,
        String reason,
        Instant occurredAt) {
}
