package com.project.common.events.notification;

import java.time.Instant;
import java.util.UUID;

public record BookingCompletedEvent(
        UUID bookingId,
        String bookingCode,
        UUID userId,
        UUID ownerId,
        UUID subFieldId,
        UUID fieldId,
        String fieldName,
        Instant occurredAt) {
}
