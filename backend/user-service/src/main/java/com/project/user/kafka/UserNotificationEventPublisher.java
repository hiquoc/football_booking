package com.project.user.kafka;

import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.ModerationNotificationEvent;
import com.project.common.events.notification.UserRequestOtpEvent;
import com.project.common.outbox.dto.OutboxSaveRequest;
import com.project.common.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserNotificationEventPublisher {

    private final OutboxService outboxService;

    public void publishUserRequestOtp(String phoneNumber) {
        UserRequestOtpEvent event = new UserRequestOtpEvent(phoneNumber, Instant.now());
        outboxService.save(new OutboxSaveRequest(
                "User",
                phoneNumber,
                event.getClass().getSimpleName(),
                NotificationEventTopics.USER_REQUEST_OTP,
                phoneNumber,
                event));
        log.info("Stored user request OTP notification outbox event: phoneNumber={}", phoneNumber);
    }

    public void publishModerationNotification(UUID userId, String code, String title, Map<String, Object> payload) {
        ModerationNotificationEvent event = new ModerationNotificationEvent(userId, null, code, title, payload, Instant.now());
        outboxService.save(new OutboxSaveRequest(
                "User",
                userId.toString(),
                event.getClass().getSimpleName(),
                NotificationEventTopics.MODERATION_NOTIFICATION,
                userId.toString(),
                event));
        log.info("Stored user moderation notification outbox event: userId={}, code={}", userId, code);
    }
}
