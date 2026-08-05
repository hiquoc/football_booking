package com.project.booking.community.kafka;

import com.project.common.events.notification.CommunityPostApplicationsHandlingRequestedEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityPostApplicationsHandlingEventConsumer {
    private final InboxService inboxService;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @KafkaListener(
            topics = NotificationEventTopics.COMMUNITY_POST_APPLICATIONS_HANDLING_REQUESTED,
            groupId = "${spring.kafka.consumer.group-id}")
    public void onApplicationsHandlingRequested(
            ConsumerRecord<String, CommunityPostApplicationsHandlingRequestedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }
}
