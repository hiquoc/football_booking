package com.project.booking.kafka;
import com.project.booking.cache.AvailabilityCacheService;
import com.project.booking.entity.Booking;
import com.project.booking.repository.BookingRepository;
import com.project.common.enums.BookingStatus;
import com.project.common.events.notification.*;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.handler.InboxEventHandler;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
@Slf4j @Component @RequiredArgsConstructor
public class PaymentInboxEventHandler implements InboxEventHandler {
    private static final Set<String> TOPICS = Set.of(NotificationEventTopics.PAYMENT_SUCCESS, NotificationEventTopics.PAYMENT_FAILED);
    private final InboxService inboxService; private final BookingRepository bookingRepository;
    private final BookingNotificationEventPublisher notificationPublisher;
    private final AvailabilityCacheService availabilityCacheService;
    @Override public boolean supports(String topic) { return TOPICS.contains(topic); }
    @Override @Transactional public void handle(InboxEvent event) {
        if (NotificationEventTopics.PAYMENT_SUCCESS.equals(event.getTopic())) {
            PaymentSuccessEvent payment = inboxService.payload(event, PaymentSuccessEvent.class);
            int changed = bookingRepository.confirmPendingBookingFromPayment(payment.bookingId(), BookingStatus.PENDING, BookingStatus.CONFIRMED);
            if (changed == 1) {
                Booking booking = bookingRepository.findById(payment.bookingId()).orElseThrow();
                availabilityCacheService.evict(booking.getSubFieldId(), booking.getBookingDate());
                notificationPublisher.publishBookingConfirmed(booking, payment.userEmail());
                log.info("Confirmed booking from payment event: bookingId={}, paymentId={}", payment.bookingId(), payment.paymentId());
            }
        } else {
            PaymentFailedEvent payment = inboxService.payload(event, PaymentFailedEvent.class);
            log.info("Payment failed; booking remains pending: bookingId={}, paymentId={}", payment.bookingId(), payment.paymentId());
        }
    }
}
