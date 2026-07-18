package com.project.common.events.notification;

import java.time.Instant;
import java.util.UUID;

public record UserCompletedBookingCountChangedEvent(
        UUID userId,
        int completedBookingCount,
        Instant occurredAt) {
}
