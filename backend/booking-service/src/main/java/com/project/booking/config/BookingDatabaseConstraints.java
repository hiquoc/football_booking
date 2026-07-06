package com.project.booking.config;

public final class BookingDatabaseConstraints {

    public static final String ACTIVE_BOOKING_OVERLAP_CONSTRAINT =
            "bookings_no_overlapping_active_bookings";

    private BookingDatabaseConstraints() {
    }
}
