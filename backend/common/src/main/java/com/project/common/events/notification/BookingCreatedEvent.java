package com.project.common.events.notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookingCreatedEvent(
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
        Long platformBookingFee,
        BigDecimal subFieldPrice,
        Long bookingPrice,
        String bookingType,
        String reservationAction,
        Instant occurredAt
) {
    public BookingCreatedEvent(
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
            Long platformBookingFee,
            BigDecimal subFieldPrice,
            Long bookingPrice,
            Instant occurredAt) {
        this(bookingId, bookingCode, userId, userEmail, ownerId, subFieldId, fieldName, bookingDate,
                startTime, endTime, platformBookingFee, subFieldPrice, bookingPrice,
                "NORMAL", null, occurredAt);
    }
}
