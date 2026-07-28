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
            topic = NotificationEventTopics.USER_BALANCE_TOP_UP_SUCCEEDED;
            event = new WalletTopUpSucceededEvent(payment.getId(),
                    payment.getBookingId(),
                    booking == null ? null : booking.getBookingCode(),
                    payment.getUserId(),
                    booking == null ? null : booking.getUserEmail(),
                    payment.getAmount(),
                    payableAmount(booking),
                    payment.getCurrency(),
                    Instant.now());
        } else {
            topic = NotificationEventTopics.PAYMENT_FAILED;
            event = new PaymentFailedEvent(payment.getId(), payment.getBookingId(), booking == null ? null : booking.getBookingCode(), payment.getUserId(),
                    booking == null ? null : booking.getUserEmail(), payment.getAmount(), payment.getFailureReason(), Instant.now());
        }
        outboxService.save(new OutboxSaveRequest("Payment", payment.getId().toString(), event.getClass().getSimpleName(),
                topic, payment.getId().toString(), event));
    }

    private Long payableAmount(BookingPaymentProjection booking) {
        if (booking == null) {
            return null;
        }
        Long bookingPrice = booking.getBookingPrice();
        if (bookingPrice != null && bookingPrice > 0) {
            return bookingPrice;
        }
        return booking.getPlatformBookingFee();
    }
}
