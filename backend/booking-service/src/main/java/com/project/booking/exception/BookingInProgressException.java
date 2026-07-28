package com.project.booking.exception;

import com.project.common.exception.ConflictException;

public class BookingInProgressException extends ConflictException {

    private static final String CODE = "BOOKING_IN_PROGRESS";

    public BookingInProgressException() {
        super("Another booking for this field is currently being processed. Please try again in a few seconds.", CODE);
    }
}
