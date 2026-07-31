package com.project.booking.kafka;

import com.project.booking.community.service.CommunityPostMaintenanceService;
import com.project.common.events.notification.BookingCancelledEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.service.InboxService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingCancelledInboxEventHandlerTest {

    @Test
    void handlerCancelsOpenCommunityPostForCancelledBookingEvent() {
        InboxService inboxService = mock(InboxService.class);
        CommunityPostMaintenanceService communityPostMaintenanceService = mock(CommunityPostMaintenanceService.class);
        BookingCancelledInboxEventHandler handler = new BookingCancelledInboxEventHandler(
                inboxService,
                communityPostMaintenanceService);
        UUID bookingId = UUID.randomUUID();
        InboxEvent envelope = InboxEvent.builder().topic(NotificationEventTopics.BOOKING_CANCELLED).build();
        BookingCancelledEvent payload = event(bookingId);
        when(inboxService.payload(envelope, BookingCancelledEvent.class)).thenReturn(payload);

        handler.handle(envelope);

        verify(communityPostMaintenanceService).cancelOpenPostForBooking(bookingId);
    }

    @Test
    void supportsOnlyBookingCancelledTopic() {
        BookingCancelledInboxEventHandler handler = new BookingCancelledInboxEventHandler(
                mock(InboxService.class),
                mock(CommunityPostMaintenanceService.class));

        assertTrue(handler.supports(NotificationEventTopics.BOOKING_CANCELLED));
        assertFalse(handler.supports(NotificationEventTopics.BOOKING_CONFIRMED));
    }

    @Test
    void consumerStoresBookingCancelledEventInInbox() {
        InboxService inboxService = mock(InboxService.class);
        BookingCancelledEventConsumer consumer = new BookingCancelledEventConsumer(inboxService);
        ConsumerRecord<String, BookingCancelledEvent> record = new ConsumerRecord<>(
                NotificationEventTopics.BOOKING_CANCELLED,
                0,
                1L,
                UUID.randomUUID().toString(),
                event(UUID.randomUUID()));

        consumer.onBookingCancelled(record);

        verify(inboxService).receive(record, null);
        assertEquals(NotificationEventTopics.BOOKING_CANCELLED, record.topic());
    }

    private BookingCancelledEvent event(UUID bookingId) {
        return new BookingCancelledEvent(
                bookingId,
                "BK-1",
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                "Change",
                "CLIENT",
                Instant.now());
    }
}
