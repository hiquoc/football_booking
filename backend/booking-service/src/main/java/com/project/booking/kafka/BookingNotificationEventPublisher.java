package com.project.booking.kafka;

import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.entity.Booking;
import com.project.common.events.notification.BookingCancelledEvent;
import com.project.common.events.notification.BookingConfirmedEvent;
import com.project.common.events.notification.BookingCreatedEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.outbox.dto.OutboxSaveRequest;
import com.project.common.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingNotificationEventPublisher {

    private final OutboxService outboxService;

    public void publishBookingCreated(Booking booking, SubFieldResponse subField, String userEmail) {
        save(booking, NotificationEventTopics.BOOKING_CREATED, new BookingCreatedEvent(
                        booking.getId(),
                        booking.getBookingCode(),
                        booking.getClientId(),
                        userEmail,
                        booking.getOwnerId(),
                        booking.getSubFieldId(),
                        subField.getFieldName(),
                        booking.getBookingDate(),
                        booking.getStartTime(),
                        booking.getEndTime(),
                        booking.getPlatformBookingFee(),
                        booking.getSubFieldPrice(),
                        booking.getBookingPrice(),
                        booking.getBookingType().name(),
                        null,
                        Instant.now()));
        log.info("Stored booking created notification outbox event: bookingId={}", booking.getId());
    }

    public void publishReservationChanged(Booking booking, SubFieldResponse subField, String action) {
        save(booking, NotificationEventTopics.BOOKING_CREATED, new BookingCreatedEvent(
                        booking.getId(),
                        booking.getBookingCode(),
                        booking.getClientId(),
                        null,
                        booking.getOwnerId(),
                        booking.getSubFieldId(),
                        subField.getFieldName(),
                        booking.getBookingDate(),
                        booking.getStartTime(),
                        booking.getEndTime(),
                        booking.getPlatformBookingFee(),
                        booking.getSubFieldPrice(),
                        booking.getBookingPrice(),
                        booking.getBookingType().name(),
                        action,
                        Instant.now()));
        log.info("Stored reservation {} notification outbox event: reservationId={}", action, booking.getId());
    }

    public void publishBookingCancelled(Booking booking, String userEmail) {
        save(booking, NotificationEventTopics.BOOKING_CANCELLED, new BookingCancelledEvent(
                        booking.getId(),
                        booking.getBookingCode(),
                        booking.getClientId(),
                        userEmail,
                        booking.getOwnerId(),
                        booking.getSubFieldId(),
                        null,
                        booking.getBookingDate(),
                        booking.getStartTime(),
                        booking.getEndTime(),
                        booking.getCancellationReason(),
                        booking.getCancelledBy() != null ? booking.getCancelledBy().name() : null,
                        booking.getBookingType().name(),
                        Instant.now()));
        log.info("Stored booking cancelled notification outbox event: bookingId={}", booking.getId());
    }

    public void publishBookingConfirmed(Booking booking, String userEmail) {
        save(booking, NotificationEventTopics.BOOKING_CONFIRMED, new BookingConfirmedEvent(
                        booking.getId(),
                        booking.getBookingCode(),
                        booking.getClientId(),
                        userEmail,
                        booking.getOwnerId(),
                        booking.getSubFieldId(),
                        null,
                        booking.getBookingDate(),
                        booking.getStartTime(),
                        booking.getEndTime(),
                        booking.getSubFieldPrice(),
                        booking.getBookingType().name(),
                        Instant.now()));
        log.info("Stored booking confirmed notification outbox event: bookingId={}", booking.getId());
    }

    private void save(Booking booking, String topic, Object payload) {
        outboxService.save(new OutboxSaveRequest(
                "Booking",
                booking.getId().toString(),
                payload.getClass().getSimpleName(),
                topic,
                booking.getId().toString(),
                payload));
    }
}
