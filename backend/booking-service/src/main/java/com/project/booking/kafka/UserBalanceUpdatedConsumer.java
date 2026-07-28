package com.project.booking.kafka;

import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.UserBalanceUpdatedEvent;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserBalanceUpdatedConsumer {
    private final InboxService inboxService;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @KafkaListener(topics = NotificationEventTopics.USER_BALANCE_UPDATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onBalanceUpdated(ConsumerRecord<String, UserBalanceUpdatedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }
}
