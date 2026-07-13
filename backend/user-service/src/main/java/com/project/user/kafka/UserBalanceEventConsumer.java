package com.project.user.kafka;

import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.UserBalanceDeductionRequestedEvent;
import com.project.common.events.notification.UserBalanceRefundRequestedEvent;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserBalanceEventConsumer {
    private final InboxService inboxService;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @KafkaListener(topics = NotificationEventTopics.USER_BALANCE_REFUND_REQUESTED, groupId = "${spring.kafka.consumer.group-id}")
    public void onRefundRequested(ConsumerRecord<String, UserBalanceRefundRequestedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }

    @KafkaListener(topics = NotificationEventTopics.USER_BALANCE_DEDUCTION_REQUESTED, groupId = "${spring.kafka.consumer.group-id}")
    public void onDeductionRequested(ConsumerRecord<String, UserBalanceDeductionRequestedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }
}
