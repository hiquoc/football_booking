package com.project.common.outbox.dto;

public record OutboxSaveRequest(
        String aggregateType,
        String aggregateId,
        String eventType,
        String topic,
        String eventKey,
        Object payload
) {
}
