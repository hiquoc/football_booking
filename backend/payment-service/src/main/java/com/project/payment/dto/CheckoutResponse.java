package com.project.payment.dto;
import java.util.UUID;
public record CheckoutResponse(UUID paymentId, String checkoutUrl) {}
