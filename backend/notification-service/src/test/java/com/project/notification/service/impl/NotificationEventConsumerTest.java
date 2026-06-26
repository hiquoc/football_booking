package com.project.notification.service.impl;

import com.project.common.events.notification.BookingConfirmedEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.service.InboxService;
import com.project.notification.dto.NotificationRequest;
import com.project.notification.enums.NotificationChannel;
import com.project.notification.enums.NotificationCode;
import com.project.notification.kafka.NotificationInboxEventHandler;
import com.project.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationEventConsumerTest {

    @Test
    void bookingConfirmedInboxEventCreatesInAppAndEmailNotification() {
        InboxService inboxService = mock(InboxService.class);
        NotificationService notificationService = mock(NotificationService.class);
        NotificationInboxEventHandler handler = new NotificationInboxEventHandler(inboxService, notificationService);
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        InboxEvent inboxEvent = InboxEvent.builder()
                .topic(NotificationEventTopics.BOOKING_CONFIRMED)
                .build();
        BookingConfirmedEvent event = new BookingConfirmedEvent(
                bookingId,
                "BK-1",
                userId,
                "user@example.com",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Stadium A",
                LocalDate.now().plusDays(1),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                BigDecimal.TEN,
                Instant.now());
        when(inboxService.payload(inboxEvent, BookingConfirmedEvent.class)).thenReturn(event);

        handler.handle(inboxEvent);

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService).create(captor.capture());
        NotificationRequest request = captor.getValue();
        assertThat(request.getUserId()).isEqualTo(userId);
        assertThat(request.getCode()).isEqualTo(NotificationCode.BOOKING_CONFIRMED);
        assertThat(request.getChannels()).containsExactly(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
        assertThat(request.getPayload()).containsEntry("bookingId", bookingId);
    }
}
