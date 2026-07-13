package com.project.user.kafka;

import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.PaymentFailedEvent;
import com.project.common.events.notification.PaymentSuccessEvent;
import com.project.common.outbox.dto.OutboxSaveRequest;
import com.project.common.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserBalanceEventPublisher {
    private final OutboxService outboxService;

    public void publishPaymentSuccess(PaymentSuccessEvent event) {
        outboxService.save(new OutboxSaveRequest(
                "UserBalance",
                event.bookingId().toString(),
                event.getClass().getSimpleName(),
                NotificationEventTopics.PAYMENT_SUCCESS,
                event.bookingId().toString(),
                event));
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        outboxService.save(new OutboxSaveRequest(
                "UserBalance",
                event.bookingId().toString(),
                event.getClass().getSimpleName(),
                NotificationEventTopics.PAYMENT_FAILED,
                event.bookingId().toString(),
                event));
    }
}
