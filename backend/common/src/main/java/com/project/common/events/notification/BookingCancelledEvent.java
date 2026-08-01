package com.project.common.events.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingCancelledEvent(
        UUID bookingId,
        String bookingCode,
        UUID userId,
        String userEmail,
        UUID ownerId,
        UUID subFieldId,
        String fieldName,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        String reason,
        String cancelledBy,
        String bookingType,
        Instant occurredAt
) {
    public BookingCancelledEvent(
            UUID bookingId,
            String bookingCode,
            UUID userId,
            String userEmail,
            UUID ownerId,
            UUID subFieldId,
            String fieldName,
            LocalDate bookingDate,
            LocalTime startTime,
            LocalTime endTime,
            String reason,
            String cancelledBy,
            Instant occurredAt) {
        this(bookingId, bookingCode, userId, userEmail, ownerId, subFieldId, fieldName, bookingDate,
                startTime, endTime, reason, cancelledBy, "NORMAL", occurredAt);
    }
}
