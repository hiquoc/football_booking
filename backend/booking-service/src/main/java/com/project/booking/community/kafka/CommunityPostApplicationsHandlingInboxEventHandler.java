package com.project.booking.community.kafka;

import com.project.booking.community.service.CommunityPostApplicationHandlingService;
import com.project.common.events.notification.CommunityPostApplicationsHandlingRequestedEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.handler.InboxEventHandler;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CommunityPostApplicationsHandlingInboxEventHandler implements InboxEventHandler {
    private final InboxService inboxService;
    private final CommunityPostApplicationHandlingService applicationHandlingService;

    @Override
    public boolean supports(String topic) {
        return NotificationEventTopics.COMMUNITY_POST_APPLICATIONS_HANDLING_REQUESTED.equals(topic);
    }

    @Override
    @Transactional
    public void handle(InboxEvent event) {
        CommunityPostApplicationsHandlingRequestedEvent payload =
                inboxService.payload(event, CommunityPostApplicationsHandlingRequestedEvent.class);
        applicationHandlingService.handlePostClosed(
                payload.postId(),
                payload.notificationCode(),
                payload.notificationTitle(),
                payload.phase(),
                payload.page());
    }
}
