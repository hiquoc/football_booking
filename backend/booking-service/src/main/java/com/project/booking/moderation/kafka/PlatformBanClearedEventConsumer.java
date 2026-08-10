package com.project.booking.moderation.kafka;

import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.PlatformBanClearedEvent;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformBanClearedEventConsumer {
    private final InboxService inboxService;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @KafkaListener(topics = NotificationEventTopics.PLATFORM_BAN_CLEARED, groupId = "${spring.kafka.consumer.group-id}")
    public void onPlatformBanCleared(ConsumerRecord<String, PlatformBanClearedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }
}
