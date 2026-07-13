package com.project.booking.payment;

import com.project.common.enums.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookingPaymentStrategyFactoryTest {

    @Test
    void selectsStrategyByPaymentMethodAndDefaultsNullToStripe() {
        BookingPaymentStrategy stripe = mock(BookingPaymentStrategy.class);
        BookingPaymentStrategy balance = mock(BookingPaymentStrategy.class);
        when(stripe.method()).thenReturn(PaymentMethod.STRIPE);
        when(balance.method()).thenReturn(PaymentMethod.ACCOUNT_BALANCE);
        BookingPaymentStrategyFactory factory = new BookingPaymentStrategyFactory(List.of(stripe, balance));

        assertSame(stripe, factory.get(PaymentMethod.STRIPE));
        assertSame(balance, factory.get(PaymentMethod.ACCOUNT_BALANCE));
        assertSame(stripe, factory.get(null));
    }
}
