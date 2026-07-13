package com.project.payment.strategy;
import com.project.payment.enums.PaymentStatus;
public record ProviderWebhookResult(String eventId, String eventType, String sessionId,
                                    String providerPaymentId, PaymentStatus status, String failureReason) {
    public boolean actionable() { return sessionId != null && status != null; }
}
