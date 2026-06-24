package com.project.booking.exception;

import com.project.common.exception.ConflictException;

public class BookingConflictException extends ConflictException {

    private static final String CODE = "BOOKING_CONFLICT";

    public BookingConflictException(String message) {
        super(message, CODE);
    }
}
