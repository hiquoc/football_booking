package com.project.common.events.notification;

import java.time.Instant;
import java.util.UUID;

public record PlatformBanRequestedEvent(
        UUID userId,
        String reason,
        UUID requestedBy,
        String source,
        Instant occurredAt) {
}
