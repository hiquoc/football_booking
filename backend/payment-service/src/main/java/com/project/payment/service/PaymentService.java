package com.project.payment.service;
import com.project.payment.dto.*;
import java.util.UUID;
public interface PaymentService {
    CheckoutResponse createCheckout(UUID userId, CreateCheckoutRequest request);
    PaymentResponse getByBookingId(UUID userId, UUID bookingId);
    void processWebhook(String payload, String signature);
}
