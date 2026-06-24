package com.project.booking.exception;

import com.project.common.exception.NotFoundException;

import java.util.UUID;

public class BookingNotFoundException extends NotFoundException {

    public BookingNotFoundException(UUID bookingId) {
        super("Booking not found with id: " + bookingId);
    }

    public BookingNotFoundException(String bookingCode) {
        super("Booking not found with code: " + bookingCode);
    }
}
