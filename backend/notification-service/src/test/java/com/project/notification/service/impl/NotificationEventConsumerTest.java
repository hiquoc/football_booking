package com.project.notification.service.impl;

import com.project.common.events.notification.BookingConfirmedEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.UserBalanceUpdatedEvent;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.service.InboxService;
import com.project.notification.dto.NotificationRequest;
import com.project.notification.dto.UserBalanceUpdateMessage;
import com.project.notification.enums.NotificationChannel;
import com.project.notification.enums.NotificationCode;
import com.project.notification.kafka.NotificationInboxEventHandler;
import com.project.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationEventConsumerTest {

    @Test
    void bookingConfirmedInboxEventCreatesInAppAndEmailNotification() {
        InboxService inboxService = mock(InboxService.class);
        NotificationService notificationService = mock(NotificationService.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        NotificationInboxEventHandler handler = new NotificationInboxEventHandler(
                inboxService,
                notificationService,
                messagingTemplate);
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

    @Test
    void userBalanceUpdatedInboxEventSendsBalanceSocketMessage() {
        InboxService inboxService = mock(InboxService.class);
        NotificationService notificationService = mock(NotificationService.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        NotificationInboxEventHandler handler = new NotificationInboxEventHandler(
                inboxService,
                notificationService,
                messagingTemplate);
        UUID userId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        InboxEvent inboxEvent = InboxEvent.builder()
                .topic(NotificationEventTopics.USER_BALANCE_UPDATED)
                .build();
        UserBalanceUpdatedEvent event = new UserBalanceUpdatedEvent(
                userId,
                150_000L,
                "WALLET_TOP_UP",
                occurredAt);
        when(inboxService.payload(inboxEvent, UserBalanceUpdatedEvent.class)).thenReturn(event);

        handler.handle(inboxEvent);

        ArgumentCaptor<UserBalanceUpdateMessage> captor = ArgumentCaptor.forClass(UserBalanceUpdateMessage.class);
        verify(messagingTemplate).convertAndSendToUser(eq(userId.toString()), eq("/queue/balance"), captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().balance()).isEqualTo(150_000L);
        assertThat(captor.getValue().reason()).isEqualTo("WALLET_TOP_UP");
        assertThat(captor.getValue().occurredAt()).isEqualTo(occurredAt);
    }
}
