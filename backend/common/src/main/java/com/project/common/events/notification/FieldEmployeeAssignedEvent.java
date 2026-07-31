package com.project.common.events.notification;

import java.time.Instant;
import java.util.UUID;

public record FieldEmployeeAssignedEvent(
        UUID assignmentId,
        UUID fieldId,
        String fieldName,
        UUID ownerId,
        UUID employeeId,
        String employeeEmail,
        Instant occurredAt
) {
}
