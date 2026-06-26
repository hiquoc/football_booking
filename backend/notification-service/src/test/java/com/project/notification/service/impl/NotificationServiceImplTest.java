package com.project.notification.service.impl;

import com.project.notification.dto.NotificationRequest;
import com.project.notification.entity.Notification;
import com.project.notification.enums.NotificationChannel;
import com.project.notification.enums.NotificationCode;
import com.project.notification.mapper.NotificationMapper;
import com.project.notification.repository.NotificationRepository;
import com.project.notification.service.NotificationSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private NotificationSender inAppSender;

    @Mock
    private NotificationSender emailSender;

    @Test
    void createPersistsNotificationAndSendsConfiguredChannels() {
        UUID userId = UUID.randomUUID();
        NotificationMapper mapper = new NotificationMapper();
        when(inAppSender.getChannel()).thenReturn(NotificationChannel.IN_APP);
        when(emailSender.getChannel()).thenReturn(NotificationChannel.EMAIL);
        NotificationServiceImpl service = new NotificationServiceImpl(
                notificationRepository,
                mapper,
                messagingTemplate,
                List.of(inAppSender, emailSender));

        when(notificationRepository.save(org.mockito.ArgumentMatchers.any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification notification = invocation.getArgument(0);
                    notification.setId(UUID.randomUUID());
                    notification.setCreatedAt(LocalDateTime.now());
                    notification.setIsRead(false);
                    return notification;
                });

        var response = service.create(NotificationRequest.builder()
                .userId(userId)
                .recipientEmail("user@example.com")
                .code(NotificationCode.BOOKING_CONFIRMED)
                .title("Booking confirmed")
                .payload(Map.of("bookingId", "123"))
                .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                .build());

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getIsRead()).isFalse();
        verify(messagingTemplate).convertAndSend(eq("/topic/users/" + userId), eq(response));
        verify(inAppSender).send(org.mockito.ArgumentMatchers.any(NotificationRequest.class));
        verify(emailSender).send(org.mockito.ArgumentMatchers.any(NotificationRequest.class));
    }

    @Test
    void markAsReadOnlyUpdatesOwnedNotification() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notificationId)
                .userId(userId)
                .code(NotificationCode.PAYMENT_SUCCESS)
                .title("Payment successful")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        NotificationServiceImpl service = new NotificationServiceImpl(
                notificationRepository,
                new NotificationMapper(),
                messagingTemplate,
                List.of());

        var response = service.markAsRead(userId, notificationId);

        assertThat(response.getIsRead()).isTrue();
        assertThat(response.getReadAt()).isNotNull();
    }

    @Test
    void markAllAsReadUpdatesUnreadNotificationsForUser() {
        UUID userId = UUID.randomUUID();
        Notification first = Notification.builder().userId(userId).isRead(false).build();
        Notification second = Notification.builder().userId(userId).isRead(false).build();
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(first, second));
        NotificationServiceImpl service = new NotificationServiceImpl(
                notificationRepository,
                new NotificationMapper(),
                messagingTemplate,
                List.of());

        service.markAllAsRead(userId);

        assertThat(first.getIsRead()).isTrue();
        assertThat(second.getIsRead()).isTrue();
        assertThat(first.getReadAt()).isNotNull();
        assertThat(second.getReadAt()).isNotNull();
    }
}
