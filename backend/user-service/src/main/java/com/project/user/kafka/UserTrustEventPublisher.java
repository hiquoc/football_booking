package com.project.user.kafka;

import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.PlatformBanClearedEvent;
import com.project.common.events.notification.UserCompletedBookingCountChangedEvent;
import com.project.common.outbox.dto.OutboxSaveRequest;
import com.project.common.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserTrustEventPublisher {
    private final OutboxService outboxService;

    public void publishCompletedBookingCountChanged(UserCompletedBookingCountChangedEvent event) {
        outboxService.save(new OutboxSaveRequest(
                "User",
                event.userId().toString(),
                event.getClass().getSimpleName(),
                NotificationEventTopics.USER_COMPLETED_BOOKING_COUNT_CHANGED,
                event.userId().toString(),
                event));
    }

    public void publishPlatformBanCleared(PlatformBanClearedEvent event) {
        outboxService.save(new OutboxSaveRequest(
                "User",
                event.userId().toString(),
                event.getClass().getSimpleName(),
                NotificationEventTopics.PLATFORM_BAN_CLEARED,
                event.userId().toString(),
                event));
    }
}
