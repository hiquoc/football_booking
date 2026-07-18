package com.project.common.events.notification;

import java.time.Instant;
import java.util.UUID;

public record PlayerMatchStatisticsAdjustedEvent(
        UUID userId,
        int totalMatchesDelta,
        int winsDelta,
        int lossesDelta,
        int drawsDelta,
        Instant occurredAt) {
}
