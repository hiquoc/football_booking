package com.project.booking.community.kafka;

import com.project.common.events.notification.CommunityNotificationEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.outbox.dto.OutboxSaveRequest;
import com.project.common.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommunityNotificationEventPublisher {
    private final OutboxService outboxService;

    public void publish(UUID userId, String code, String title, Map<String, Object> payload) {
        CommunityNotificationEvent event = new CommunityNotificationEvent(userId, null, code, title, payload, Instant.now());
        outboxService.save(new OutboxSaveRequest(
                "Community",
                String.valueOf(payload.getOrDefault("postId", userId)),
                event.getClass().getSimpleName(),
                NotificationEventTopics.COMMUNITY_NOTIFICATION,
                userId.toString(),
                event));
    }
}
