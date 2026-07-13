package com.project.booking.payment;

import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.entity.Booking;
import com.project.booking.kafka.BookingBalanceEventPublisher;
import com.project.common.enums.PaymentMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BalanceBookingPaymentStrategy implements BookingPaymentStrategy {
    private static final String REASON = "BOOKING_ACCOUNT_BALANCE_PAYMENT";

    private final BookingBalanceEventPublisher balanceEventPublisher;

    @Override
    public PaymentMethod method() {
        return PaymentMethod.ACCOUNT_BALANCE;
    }

    @Override
    public void onBookingCreated(Booking booking, SubFieldResponse subField) {
        balanceEventPublisher.publishDeductionRequested(booking, REASON);
    }
}
