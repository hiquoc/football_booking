package com.project.booking.kafka;

import com.project.booking.cache.AvailabilityCacheService;
import com.project.booking.entity.Booking;
import com.project.booking.repository.BookingRepository;
import com.project.booking.service.PendingBookingReservationService;
import com.project.common.enums.BookingPaymentStatus;
import com.project.common.enums.BookingStatus;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.PaymentFailedEvent;
import com.project.common.events.notification.PaymentSuccessEvent;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.handler.InboxEventHandler;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentInboxEventHandler implements InboxEventHandler {
    private static final Set<String> TOPICS = Set.of(
            NotificationEventTopics.PAYMENT_SUCCESS,
            NotificationEventTopics.PAYMENT_FAILED);

    private final InboxService inboxService;
    private final BookingRepository bookingRepository;
    private final BookingNotificationEventPublisher notificationPublisher;
    private final BookingBalanceEventPublisher balanceEventPublisher;
    private final AvailabilityCacheService availabilityCacheService;
    private final PendingBookingReservationService pendingBookingReservationService;

    @Override
    public boolean supports(String topic) {
        return TOPICS.contains(topic);
    }

    @Override
    @Transactional
    public void handle(InboxEvent event) {
        if (NotificationEventTopics.PAYMENT_SUCCESS.equals(event.getTopic())) {
            PaymentSuccessEvent payment = inboxService.payload(event, PaymentSuccessEvent.class);
            handlePaymentSuccessFallback(payment);
            return;
        }

        PaymentFailedEvent payment = inboxService.payload(event, PaymentFailedEvent.class);
        log.info("Payment failed; booking remains pending: bookingId={}, paymentId={}",
                payment.bookingId(), payment.paymentId());
    }

    private void handlePaymentSuccessFallback(PaymentSuccessEvent payment) {
        int changed = bookingRepository.confirmPendingBookingFromPayment(
                payment.bookingId(), BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingPaymentStatus.PAID);
        if (changed != 1) {
            refundPaymentIfBookingCannotBeConfirmed(payment);
            return;
        }

        Booking booking = bookingRepository.findById(payment.bookingId()).orElseThrow();
        pendingBookingReservationService.release(booking);
        availabilityCacheService.evict(booking.getSubFieldId(), booking.getBookingDate());
        notificationPublisher.publishBookingConfirmed(booking, payment.userEmail());
        log.info("Confirmed pending booking from payment success fallback: bookingId={}, paymentId={}",
                payment.bookingId(), payment.paymentId());
    }

    private void refundPaymentIfBookingCannotBeConfirmed(PaymentSuccessEvent payment) {
        Booking booking = bookingRepository.findById(payment.bookingId()).orElseThrow();
        if (booking.getStatus() != BookingStatus.CANCELLED && booking.getStatus() != BookingStatus.EXPIRED) {
            log.info("Payment success fallback skipped because booking is not pending: bookingId={}, paymentId={}, status={}",
                    payment.bookingId(), payment.paymentId(), booking.getStatus());
            return;
        }

        long amount = payment.amount().longValueExact();
        balanceEventPublisher.publishRefundRequested(booking, amount, "BOOKING_PAYMENT_REFUND");
        bookingRepository.markPaymentRefunded(booking.getId(), BookingPaymentStatus.REFUNDED);
        log.warn("Refund requested for paid booking that can no longer be confirmed: bookingId={}, paymentId={}, status={}",
                payment.bookingId(), payment.paymentId(), booking.getStatus());
    }
}
