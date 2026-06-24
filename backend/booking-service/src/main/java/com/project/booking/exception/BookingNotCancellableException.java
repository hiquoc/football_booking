package com.project.booking.exception;

import com.project.common.exception.BadRequestException;
import java.util.UUID;

public class BookingNotCancellableException extends BadRequestException {

    public BookingNotCancellableException(UUID bookingId, String currentStatus) {
        super("Booking with id " + bookingId + " cannot be cancelled. Current status: " + currentStatus);
    }
}
