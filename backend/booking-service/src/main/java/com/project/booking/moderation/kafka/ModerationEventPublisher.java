package com.project.booking.moderation.kafka;

import com.project.common.events.notification.ModerationNotificationEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.PlatformBanRequestedEvent;
import com.project.common.outbox.dto.OutboxSaveRequest;
import com.project.common.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModerationEventPublisher {
    private final OutboxService outboxService;

    public void publishNotification(ModerationNotificationEvent event) {
        outboxService.save(new OutboxSaveRequest(
                "Moderation",
                event.userId().toString(),
                event.getClass().getSimpleName(),
                NotificationEventTopics.MODERATION_NOTIFICATION,
                event.userId().toString(),
                event));
    }

    public void publishPlatformBanRequested(PlatformBanRequestedEvent event) {
        outboxService.save(new OutboxSaveRequest(
                "Moderation",
                event.userId().toString(),
                event.getClass().getSimpleName(),
                NotificationEventTopics.PLATFORM_BAN_REQUESTED,
                event.userId().toString(),
                event));
    }
}
