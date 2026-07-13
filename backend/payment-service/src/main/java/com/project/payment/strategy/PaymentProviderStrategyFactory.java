package com.project.payment.strategy;
import com.project.common.exception.BadRequestException;
import com.project.payment.enums.PaymentProvider;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class PaymentProviderStrategyFactory {
    private final Map<PaymentProvider, PaymentProviderStrategy> strategies;
    public PaymentProviderStrategyFactory(List<PaymentProviderStrategy> strategies) {
        EnumMap<PaymentProvider, PaymentProviderStrategy> values = new EnumMap<>(PaymentProvider.class);
        strategies.forEach(strategy -> values.put(strategy.provider(), strategy));
        this.strategies = Map.copyOf(values);
    }
    public PaymentProviderStrategy get(PaymentProvider provider) {
        PaymentProviderStrategy strategy = strategies.get(provider);
        if (strategy == null) throw new BadRequestException("Unsupported payment provider: " + provider);
        return strategy;
    }
}
