package com.project.booking.kafka;

import com.project.booking.entity.Booking;
import com.project.common.events.notification.BookingCompletedEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.outbox.dto.OutboxSaveRequest;
import com.project.common.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class BookingTrustEventPublisher {
    private final OutboxService outboxService;

    public void publishBookingCompleted(Booking booking) {
        BookingCompletedEvent event = new BookingCompletedEvent(
                booking.getId(),
                booking.getBookingCode(),
                booking.getClientId(),
                booking.getOwnerId(),
                booking.getSubFieldId(),
                booking.getSubField() != null ? booking.getSubField().getFieldId() : null,
                booking.getSubField() != null ? booking.getSubField().getFieldName() : null,
                Instant.now());
        outboxService.save(new OutboxSaveRequest(
                "Booking",
                booking.getId().toString(),
                event.getClass().getSimpleName(),
                NotificationEventTopics.BOOKING_COMPLETED,
                booking.getId().toString(),
                event));
    }
}
