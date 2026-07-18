package com.project.common.events.notification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ModerationNotificationEvent(
        UUID userId,
        String userEmail,
        String code,
        String title,
        Map<String, Object> payload,
        Instant occurredAt) {
}
