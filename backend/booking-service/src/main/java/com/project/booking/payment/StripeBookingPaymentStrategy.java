package com.project.booking.payment;

import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.entity.Booking;
import com.project.booking.kafka.BookingNotificationEventPublisher;
import com.project.common.enums.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StripeBookingPaymentStrategy implements BookingPaymentStrategy {
    private final BookingNotificationEventPublisher notificationEventPublisher;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.STRIPE;
    }

    @Override
    public void onBookingCreated(Booking booking, SubFieldResponse subField) {
        notificationEventPublisher.publishBookingCreated(booking, subField, null);
    }
}
