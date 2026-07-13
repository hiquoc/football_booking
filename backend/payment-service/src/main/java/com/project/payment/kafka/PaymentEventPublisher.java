package com.project.payment.kafka;
import com.project.common.events.notification.*;
import com.project.common.outbox.dto.OutboxSaveRequest;
import com.project.common.outbox.service.OutboxService;
import com.project.payment.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;
@Component @RequiredArgsConstructor
public class PaymentEventPublisher {
    private final OutboxService outboxService;
    public void publish(Payment payment, BookingPaymentProjection booking) {
        Object event; String topic;
        if (payment.getStatus() == com.project.payment.enums.PaymentStatus.SUCCESS) {
            topic = NotificationEventTopics.PAYMENT_SUCCESS;
            event = new PaymentSuccessEvent(payment.getId(), payment.getBookingId(), booking.getBookingCode(), booking.getUserId(),
                    booking.getUserEmail(), payment.getAmount(), Instant.now());
        } else {
            topic = NotificationEventTopics.PAYMENT_FAILED;
            event = new PaymentFailedEvent(payment.getId(), payment.getBookingId(), booking.getBookingCode(), booking.getUserId(),
                    booking.getUserEmail(), payment.getAmount(), payment.getFailureReason(), Instant.now());
        }
        outboxService.save(new OutboxSaveRequest("Payment", payment.getId().toString(), event.getClass().getSimpleName(),
                topic, payment.getBookingId().toString(), event));
    }
}
