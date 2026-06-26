package com.project.common.outbox.dto;

import java.time.Instant;
import java.util.UUID;

public record OutboxDlqPayload(
        UUID eventId,
        String aggregateId,
        String aggregateType,
        String eventType,
        String originalTopic,
        String payload,
        int retryCount,
        Instant timestamp,
        String errorMessage
) {
}
