package com.project.user.kafka;

import com.project.common.events.notification.MatchEvaluationSubmittedEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.handler.InboxEventHandler;
import com.project.common.inbox.service.InboxService;
import com.project.user.service.UserReputationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserReputationInboxEventHandler implements InboxEventHandler {
    private final InboxService inboxService;
    private final UserReputationService reputationService;

    @Override
    public boolean supports(String topic) {
        return NotificationEventTopics.MATCH_EVALUATION_SUBMITTED.equals(topic);
    }

    @Override
    @Transactional
    public void handle(InboxEvent event) {
        reputationService.recordEvaluation(inboxService.payload(event, MatchEvaluationSubmittedEvent.class));
    }
}
