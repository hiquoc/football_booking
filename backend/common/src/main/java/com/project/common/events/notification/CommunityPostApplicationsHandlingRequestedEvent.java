package com.project.common.events.notification;

import java.time.Instant;
import java.util.UUID;

public record CommunityPostApplicationsHandlingRequestedEvent(
        UUID postId,
        String notificationCode,
        String notificationTitle,
        String phase,
        int page,
        Instant occurredAt
) {
}
