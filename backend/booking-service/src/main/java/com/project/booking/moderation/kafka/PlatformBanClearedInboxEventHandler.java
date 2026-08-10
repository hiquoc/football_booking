package com.project.booking.moderation.kafka;

import com.project.booking.moderation.service.BookingModerationService;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.PlatformBanClearedEvent;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.handler.InboxEventHandler;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PlatformBanClearedInboxEventHandler implements InboxEventHandler {
    private final InboxService inboxService;
    private final BookingModerationService moderationService;

    @Override
    public boolean supports(String topic) {
        return NotificationEventTopics.PLATFORM_BAN_CLEARED.equals(topic);
    }

    @Override
    @Transactional
    public void handle(InboxEvent event) {
        PlatformBanClearedEvent payload = inboxService.payload(event, PlatformBanClearedEvent.class);
        moderationService.resetPlatformBan(payload.clearedBy(), payload.userId());
    }
}
