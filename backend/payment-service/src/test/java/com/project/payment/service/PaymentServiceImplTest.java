package com.project.payment.service;

import com.project.payment.dto.CreateCheckoutRequest;
import com.project.payment.entity.*;
import com.project.payment.enums.*;
import com.project.payment.kafka.PaymentEventPublisher;
import com.project.payment.repository.*;
import com.project.payment.strategy.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceImplTest {
    private PaymentRepository payments;
    private BookingPaymentProjectionRepository bookings;
    private PaymentSessionRepository sessions;
    private ProviderWebhookEventRepository webhookEvents;
    private PaymentEventPublisher publisher;
    private PaymentProviderStrategy stripe;
    private PaymentServiceImpl service;
    private UUID userId;
    private UUID bookingId;

    @BeforeEach void setUp() {
        payments = mock(PaymentRepository.class); bookings = mock(BookingPaymentProjectionRepository.class);
        sessions = mock(PaymentSessionRepository.class); stripe = mock(PaymentProviderStrategy.class);
        webhookEvents = mock(ProviderWebhookEventRepository.class); publisher = mock(PaymentEventPublisher.class);
        when(stripe.provider()).thenReturn(PaymentProvider.STRIPE);
        PaymentProviderStrategyFactory factory = new PaymentProviderStrategyFactory(List.of(stripe));
        service = new PaymentServiceImpl(payments, bookings, webhookEvents, sessions, factory, publisher);
        userId = UUID.randomUUID(); bookingId = UUID.randomUUID();
        when(bookings.findById(bookingId)).thenReturn(Optional.of(BookingPaymentProjection.builder()
                .bookingId(bookingId).bookingCode("BK-1").userId(userId)
                .subFieldPrice(new BigDecimal("300000.00"))
                .bookingPrice(5000L)
                .platformBookingFee(5000L)
                .build()));
        when(payments.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(payments.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment value = invocation.getArgument(0);
            if (value.getId() == null) value.setId(UUID.randomUUID());
            return value;
        });
    }

    @Test void checkoutUsesRequestedWalletTopUpAmountAndSelectedStrategy() {
        when(stripe.createCheckout(any())).thenReturn(new ProviderCheckoutResult("cs_test_1", "https://checkout.stripe.com/test", java.time.Instant.now().plusSeconds(1800)));
        var response = service.createCheckout(userId,
                new CreateCheckoutRequest(bookingId, new BigDecimal("5000"), "vnd", PaymentProvider.STRIPE, null));
        assertEquals("https://checkout.stripe.com/test", response.checkoutUrl());
        verify(stripe).createCheckout(argThat(request -> request.amount().compareTo(new BigDecimal("5000")) == 0
                && request.currency().equals("VND") && request.attempt() == 1));
        verify(sessions).save(any(PaymentSession.class));
    }

    @Test void checkoutAllowsShortfallWalletTopUpAmount() {
        when(stripe.createCheckout(any())).thenReturn(new ProviderCheckoutResult("cs_test_2", "https://checkout.stripe.com/shortfall", java.time.Instant.now().plusSeconds(1800)));

        var response = service.createCheckout(userId,
                new CreateCheckoutRequest(bookingId, BigDecimal.ONE, "VND", PaymentProvider.STRIPE, null));

        assertEquals("https://checkout.stripe.com/shortfall", response.checkoutUrl());
        verify(stripe).createCheckout(argThat(request -> request.amount().compareTo(BigDecimal.ONE) == 0));
    }

    @Test void checkoutPassesSafeReturnPathToProvider() {
        when(stripe.createCheckout(any())).thenReturn(new ProviderCheckoutResult("cs_test_3", "https://checkout.stripe.com/return", java.time.Instant.now().plusSeconds(1800)));

        service.createCheckout(userId,
                new CreateCheckoutRequest(null, new BigDecimal("20000"), "VND", PaymentProvider.STRIPE,
                        "/fields/field-1/book?date=2026-08-10&slot=2026-08-10T18%3A00"));

        verify(stripe).createCheckout(argThat(request -> "/fields/field-1/book?date=2026-08-10&slot=2026-08-10T18%3A00".equals(request.returnPath())));
    }

    @Test void checkoutRejectsUnsafeReturnPath() {
        assertThrows(com.project.common.exception.BadRequestException.class, () -> service.createCheckout(userId,
                new CreateCheckoutRequest(null, new BigDecimal("20000"), "VND", PaymentProvider.STRIPE,
                        "//evil.example")));
        verify(stripe, never()).createCheckout(any());
    }

    @Test void successfulWebhookUpdatesPaymentAndPublishesOutboxEvent() {
        Payment payment = Payment.builder().id(UUID.randomUUID()).bookingId(bookingId).userId(userId)
                .provider(PaymentProvider.STRIPE).amount(new BigDecimal("300000")).currency("VND")
                .status(PaymentStatus.PENDING).checkoutAttempt(1).build();
        when(stripe.parseWebhook("payload", "signature")).thenReturn(new ProviderWebhookResult(
                "evt_1", "checkout.session.completed", "cs_1", "pi_1", PaymentStatus.SUCCESS, null));
        when(webhookEvents.existsById("evt_1")).thenReturn(false);
        when(sessions.findById("cs_1")).thenReturn(Optional.of(PaymentSession.builder()
                .providerSessionId("cs_1").paymentId(payment.getId()).attempt(1).build()));
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));
        service.processWebhook("payload", "signature");
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        verify(publisher).publish(payment, bookings.findById(bookingId).orElseThrow());
    }

    @Test void duplicateWebhookIsIgnored() {
        when(stripe.parseWebhook("payload", "signature")).thenReturn(new ProviderWebhookResult(
                "evt_1", "checkout.session.completed", "cs_1", "pi_1", PaymentStatus.SUCCESS, null));
        when(webhookEvents.existsById("evt_1")).thenReturn(true);
        service.processWebhook("payload", "signature");
        verify(webhookEvents, never()).saveAndFlush(any());
        verifyNoInteractions(publisher);
    }
}
