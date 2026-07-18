package com.project.user.kafka;

import com.project.common.events.notification.MatchEvaluationSubmittedEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserReputationEventConsumer {
    private final InboxService inboxService;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @KafkaListener(topics = NotificationEventTopics.MATCH_EVALUATION_SUBMITTED, groupId = "${spring.kafka.consumer.group-id}")
    public void onMatchEvaluationSubmitted(ConsumerRecord<String, MatchEvaluationSubmittedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }
}
