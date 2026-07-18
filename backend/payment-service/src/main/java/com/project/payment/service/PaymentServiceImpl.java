package com.project.payment.service;

import com.project.common.exception.*;
import com.project.payment.dto.*;
import com.project.payment.entity.*;
import com.project.payment.enums.*;
import com.project.payment.kafka.PaymentEventPublisher;
import com.project.payment.repository.*;
import com.project.payment.strategy.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Slf4j @Service @RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingPaymentProjectionRepository bookingRepository;
    private final ProviderWebhookEventRepository webhookEventRepository;
    private final PaymentSessionRepository paymentSessionRepository;
    private final PaymentProviderStrategyFactory strategyFactory;
    private final PaymentEventPublisher eventPublisher;

    //Boc try catch voi tao payment moi de tra ve ket qua cu khi request chay nhieu lan
    @Override @Transactional
    public CheckoutResponse createCheckout(UUID userId, CreateCheckoutRequest request) {
        BookingPaymentProjection booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> new NotFoundException("Booking is not available for payment"));
        if (!booking.getUserId().equals(userId)) throw new ForbiddenException("You are not authorised to pay for this booking");
        java.math.BigDecimal payableAmount = java.math.BigDecimal.valueOf(booking.getBookingPrice() == null ? 0L : booking.getBookingPrice());
        if (payableAmount.compareTo(request.amount()) != 0) throw new BadRequestException("Payment amount does not match booking total");
        String currency = request.currency().toUpperCase(Locale.ROOT);
        PaymentProvider provider = request.provider() == null ? PaymentProvider.STRIPE : request.provider();
        Payment payment = paymentRepository.findByBookingId(request.bookingId()).orElseGet(() -> paymentRepository.save(
                Payment.builder().bookingId(request.bookingId()).userId(userId).provider(provider)
                        .amount(payableAmount).currency(currency).status(PaymentStatus.PENDING).build()));
        if (payment.getStatus() == PaymentStatus.SUCCESS) throw new ConflictException("Booking has already been paid");
        if (payment.getProvider() != provider || !payment.getCurrency().equals(currency))
            throw new ConflictException("An existing payment uses a different provider or currency");
        if (payment.getProviderSessionId() != null && payment.getStatus() == PaymentStatus.PENDING)
            return new CheckoutResponse(payment.getId(), strategyFactory.get(provider).checkoutUrl(payment.getProviderSessionId()));
        payment.setStatus(PaymentStatus.PENDING); payment.setFailureReason(null); payment.setCheckoutAttempt(payment.getCheckoutAttempt() + 1);
        ProviderCheckoutResult result = strategyFactory.get(provider).createCheckout(new ProviderCheckoutRequest(
                payment.getId(), payment.getBookingId(), payment.getCheckoutAttempt(), booking.getBookingCode(), payment.getAmount(), currency, booking.getUserEmail()));
        payment.setProviderSessionId(result.sessionId()); payment.setExpiresAt(result.expiresAt()); paymentRepository.save(payment);
        paymentSessionRepository.save(PaymentSession.builder().providerSessionId(result.sessionId()).paymentId(payment.getId()).attempt(payment.getCheckoutAttempt()).build());
        log.info("Created {} checkout: paymentId={}, bookingId={}, sessionId={}", provider, payment.getId(), payment.getBookingId(), result.sessionId());
        return new CheckoutResponse(payment.getId(), result.checkoutUrl());
    }

    @Override @Transactional(readOnly=true)
    public PaymentResponse getByBookingId(UUID userId, UUID bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new NotFoundException("Payment not found for booking " + bookingId));
        if (!payment.getUserId().equals(userId)) throw new ForbiddenException("You are not authorised to view this payment");
        return toResponse(payment);
    }

    @Override @Transactional
    public void processWebhook(String payload, String signature) {
        PaymentProviderStrategy strategy = strategyFactory.get(PaymentProvider.STRIPE);
        ProviderWebhookResult result = strategy.parseWebhook(payload, signature);
        if (webhookEventRepository.existsById(result.eventId())) { log.info("Ignoring duplicate webhook eventId={}", result.eventId()); return; }
        try {
            webhookEventRepository.saveAndFlush(ProviderWebhookEvent.builder().eventId(result.eventId())
                    .provider(strategy.provider().name()).eventType(result.eventType()).processedAt(Instant.now()).build());
        } catch (DataIntegrityViolationException duplicate) { return; }
        if (!result.actionable()) return;
        PaymentSession session = paymentSessionRepository.findById(result.sessionId())
                .orElseThrow(() -> new NotFoundException("Payment not found for provider session"));
        Payment payment = paymentRepository.findById(session.getPaymentId()).orElseThrow();
        if (session.getAttempt() != payment.getCheckoutAttempt()) return;
        if (payment.getStatus() == PaymentStatus.SUCCESS) return;
        payment.setStatus(result.status()); payment.setProviderPaymentId(result.providerPaymentId());
        payment.setFailureReason(result.failureReason()); paymentRepository.save(payment);
        BookingPaymentProjection booking = bookingRepository.findById(payment.getBookingId()).orElseThrow();
        if (result.status() == PaymentStatus.SUCCESS || result.status() == PaymentStatus.FAILED || result.status() == PaymentStatus.CANCELLED)
            eventPublisher.publish(payment, booking);
        log.info("Processed webhook: eventId={}, paymentId={}, status={}", result.eventId(), payment.getId(), payment.getStatus());
    }

    private PaymentResponse toResponse(Payment p) { return new PaymentResponse(p.getId(), p.getBookingId(), p.getProvider(),
            p.getAmount(), p.getCurrency(), p.getStatus(), p.getFailureReason(), p.getExpiresAt(), p.getCreatedAt(), p.getUpdatedAt()); }
}
