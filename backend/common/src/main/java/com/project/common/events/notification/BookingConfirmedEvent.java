package com.project.common.events.notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingConfirmedEvent(
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
        BigDecimal subFieldPrice,
        String bookingType,
        Instant occurredAt
) {
    public BookingConfirmedEvent(
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
            BigDecimal subFieldPrice,
            Instant occurredAt) {
        this(bookingId, bookingCode, userId, userEmail, ownerId, subFieldId, fieldName, bookingDate,
                startTime, endTime, subFieldPrice, "NORMAL", occurredAt);
    }
}
