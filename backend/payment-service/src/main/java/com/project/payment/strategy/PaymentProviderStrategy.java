package com.project.payment.strategy;
import com.project.payment.enums.PaymentProvider;
public interface PaymentProviderStrategy {
    PaymentProvider provider();
    ProviderCheckoutResult createCheckout(ProviderCheckoutRequest request);
    String checkoutUrl(String sessionId);
    ProviderWebhookResult parseWebhook(String payload, String signature);
}
