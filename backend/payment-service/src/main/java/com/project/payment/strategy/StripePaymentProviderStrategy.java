package com.project.payment.strategy;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.project.common.exception.BadRequestException;
import com.project.payment.config.StripeProperties;
import com.project.payment.enums.PaymentProvider;
import com.project.payment.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Set;
import java.time.Instant;

@Component @RequiredArgsConstructor
public class StripePaymentProviderStrategy implements PaymentProviderStrategy {
    private static final Set<String> ZERO_DECIMAL_CURRENCIES = Set.of("bif", "clp", "djf", "gnf", "jpy", "kmf", "krw", "mga", "pyg", "rwf", "ugx", "vnd", "vuv", "xaf", "xof", "xpf");
    private final StripeProperties properties;

    @Override public PaymentProvider provider() { return PaymentProvider.STRIPE; }

    @Override
    public ProviderCheckoutResult createCheckout(ProviderCheckoutRequest request) {
        try {
            String currency = request.currency().toLowerCase();
            long unitAmount = (ZERO_DECIMAL_CURRENCIES.contains(currency) ? request.amount() : request.amount().movePointRight(2)).longValueExact();
            SessionCreateParams.Builder params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl(request))
                    .setCancelUrl(cancelUrl(request))
                    .setExpiresAt(Instant.now().plusSeconds(30 * 60).getEpochSecond())
                    .putMetadata("paymentId", request.paymentId().toString())
                    .addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(currency)
                                    .setUnitAmount(unitAmount)
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(request.bookingCode() == null ? "Wallet top-up" : "Wallet top-up for booking " + request.bookingCode()).build())
                                    .build()).build());
            if (request.bookingId() != null) params.putMetadata("bookingId", request.bookingId().toString());
            if (request.customerEmail() != null && !request.customerEmail().isBlank()) params.setCustomerEmail(request.customerEmail());
            RequestOptions options = RequestOptions.builder().setApiKey(properties.secretKey())
                    .setIdempotencyKey("checkout-wallet-top-up-" + request.paymentId() + "-" + request.attempt()).build();
            Session session = Session.create(params.build(), options);
            return new ProviderCheckoutResult(session.getId(), session.getUrl(), Instant.ofEpochSecond(session.getExpiresAt()));
        } catch (ArithmeticException ex) {
            throw new BadRequestException("Payment amount has unsupported precision");
        } catch (StripeException ex) {
            throw new IllegalStateException("Stripe Checkout Session creation failed", ex);
        }
    }

    @Override
    public String checkoutUrl(String sessionId) {
        try {
            return Session.retrieve(sessionId, RequestOptions.builder().setApiKey(properties.secretKey()).build()).getUrl();
        } catch (StripeException ex) {
            throw new IllegalStateException("Stripe Checkout Session retrieval failed", ex);
        }
    }

    @Override
    public ProviderWebhookResult parseWebhook(String payload, String signature) {
        final Event event;
        try {
            event = Webhook.constructEvent(payload, signature, properties.webhookSecret());
        } catch (SignatureVerificationException ex) {
            throw new BadRequestException("Invalid Stripe webhook signature");
        }
        final StripeObject object;
        try {
            object = event.getDataObjectDeserializer().getObject()
                    .orElseGet(() -> deserializeUnsafe(event));
        } catch (IllegalStateException ex) {
            throw new BadRequestException("Unable to deserialize Stripe webhook payload");
        }
        if (!(object instanceof Session session)) return ignored(event);
        return switch (event.getType()) {
            case "checkout.session.completed", "checkout.session.async_payment_succeeded" ->
                    new ProviderWebhookResult(event.getId(), event.getType(), session.getId(),
                            session.getPaymentIntent(), "paid".equals(session.getPaymentStatus()) ? PaymentStatus.SUCCESS : null, null);
            case "checkout.session.async_payment_failed" -> new ProviderWebhookResult(event.getId(), event.getType(),
                    session.getId(), session.getPaymentIntent(), PaymentStatus.FAILED, "Stripe asynchronous payment failed");
            case "checkout.session.expired" -> new ProviderWebhookResult(event.getId(), event.getType(),
                    session.getId(), session.getPaymentIntent(), PaymentStatus.CANCELLED, "Stripe Checkout Session expired");
            default -> ignored(event);
        };
    }

    private ProviderWebhookResult ignored(Event event) {
        return new ProviderWebhookResult(event.getId(), event.getType(), null, null, null, null);
    }

    private String successUrl(ProviderCheckoutRequest request) {
        return properties.frontendUrl() + appendTopUpStatus(defaultReturnPath(request), "returned");
    }

    private String cancelUrl(ProviderCheckoutRequest request) {
        return properties.frontendUrl() + appendTopUpStatus(defaultReturnPath(request), "cancelled");
    }

    private String defaultReturnPath(ProviderCheckoutRequest request) {
        if (request.returnPath() != null && !request.returnPath().isBlank()) return request.returnPath();
        return request.bookingId() == null
                ? "/profile"
                : "/bookings/" + request.bookingId() + "/payment";
    }

    private String appendTopUpStatus(String returnPath, String status) {
        String separator = returnPath.contains("?") ? "&" : "?";
        return returnPath + separator + "topup=" + status;
    }

    private StripeObject deserializeUnsafe(Event event) {
        try {
            return event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (EventDataObjectDeserializationException ex) {
            throw new IllegalStateException("Stripe event deserialization failed", ex);
        }
    }
}
