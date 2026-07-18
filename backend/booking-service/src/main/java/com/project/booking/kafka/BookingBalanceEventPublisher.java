package com.project.booking.kafka;

import com.project.booking.entity.Booking;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.UserBalanceDeductionRequestedEvent;
import com.project.common.events.notification.UserBalanceRefundRequestedEvent;
import com.project.common.outbox.dto.OutboxSaveRequest;
import com.project.common.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class BookingBalanceEventPublisher {
    private final OutboxService outboxService;

    public void publishDeductionRequested(Booking booking, String reason) {
        long payableAmount = booking.getBookingPrice() == null || booking.getBookingPrice() == 0L
                ? (booking.getPlatformBookingFee() == null ? 0L : booking.getPlatformBookingFee())
                : booking.getBookingPrice();
        UserBalanceDeductionRequestedEvent event = new UserBalanceDeductionRequestedEvent(
                booking.getClientId(),
                payableAmount,
                booking.getId(),
                booking.getBookingCode(),
                reason,
                Instant.now());
        save(booking, NotificationEventTopics.USER_BALANCE_DEDUCTION_REQUESTED, event);
    }

    public void publishRefundRequested(Booking booking, long amount, String reason) {
        if (amount <= 0) {
            return;
        }
        UserBalanceRefundRequestedEvent event = new UserBalanceRefundRequestedEvent(
                booking.getClientId(),
                amount,
                booking.getId(),
                booking.getBookingCode(),
                reason,
                Instant.now());
        save(booking, NotificationEventTopics.USER_BALANCE_REFUND_REQUESTED, event);
    }

    private void save(Booking booking, String topic, Object event) {
        outboxService.save(new OutboxSaveRequest(
                "Booking",
                booking.getId().toString(),
                event.getClass().getSimpleName(),
                topic,
                booking.getId().toString(),
                event));
    }
}
