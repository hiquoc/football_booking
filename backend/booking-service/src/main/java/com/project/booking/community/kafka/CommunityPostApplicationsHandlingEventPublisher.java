package com.project.booking.community.kafka;

import com.project.common.events.notification.CommunityPostApplicationsHandlingRequestedEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.outbox.dto.OutboxSaveRequest;
import com.project.common.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommunityPostApplicationsHandlingEventPublisher {
    private final OutboxService outboxService;

    public void publish(UUID postId, String notificationCode, String notificationTitle) {
        publish(postId, notificationCode, notificationTitle, "ACCEPTED", 0);
    }

    public void publish(UUID postId, String notificationCode, String notificationTitle, String phase, int page) {
        CommunityPostApplicationsHandlingRequestedEvent event = new CommunityPostApplicationsHandlingRequestedEvent(
                postId,
                notificationCode,
                notificationTitle,
                phase,
                page,
                Instant.now());
        outboxService.save(new OutboxSaveRequest(
                "CommunityPost",
                postId.toString(),
                event.getClass().getSimpleName(),
                NotificationEventTopics.COMMUNITY_POST_APPLICATIONS_HANDLING_REQUESTED,
                postId.toString(),
                event));
    }
}
