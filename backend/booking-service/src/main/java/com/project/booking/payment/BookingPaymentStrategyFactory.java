package com.project.booking.payment;

import com.project.common.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class BookingPaymentStrategyFactory {
    private final Map<PaymentMethod, BookingPaymentStrategy> strategies = new EnumMap<>(PaymentMethod.class);

    public BookingPaymentStrategyFactory(List<BookingPaymentStrategy> strategies) {
        strategies.forEach(strategy -> this.strategies.put(strategy.method(), strategy));
    }

    public BookingPaymentStrategy get(PaymentMethod method) {
        BookingPaymentStrategy strategy = strategies.get(method == null ? PaymentMethod.STRIPE : method);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported payment method " + method);
        }
        return strategy;
    }
}
