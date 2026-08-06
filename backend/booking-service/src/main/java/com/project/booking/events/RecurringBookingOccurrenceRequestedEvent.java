package com.project.booking.events;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record RecurringBookingOccurrenceRequestedEvent(
        UUID recurringBookingId,
        UUID userId,
        UUID subFieldId,
        LocalDate bookingDate,
        LocalTime startTime,
        int durationMinutes,
        Instant occurredAt) {
}
