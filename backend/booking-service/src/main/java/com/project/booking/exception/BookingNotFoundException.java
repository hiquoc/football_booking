package com.project.booking.exception;

import com.project.common.exception.NotFoundException;

import java.util.UUID;

public class BookingNotFoundException extends NotFoundException {

    public BookingNotFoundException(UUID bookingId) {
        super("Booking not found with id: " + bookingId, "BOOKING_NOT_FOUND");
    }

    public BookingNotFoundException(String bookingCode) {
        super("Booking not found with code: " + bookingCode, "BOOKING_NOT_FOUND");
    }
}
