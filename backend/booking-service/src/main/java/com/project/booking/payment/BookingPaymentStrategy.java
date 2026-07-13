package com.project.booking.payment;

import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.entity.Booking;
import com.project.common.enums.PaymentMethod;

public interface BookingPaymentStrategy {
    PaymentMethod method();
    void onBookingCreated(Booking booking, SubFieldResponse subField);
}
