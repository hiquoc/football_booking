package com.project.booking.kafka;
import com.project.common.events.notification.*;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class PaymentEventConsumer {
    private final InboxService inboxService;
    @Value("${spring.kafka.consumer.group-id}") private String consumerGroup;
    @KafkaListener(topics=NotificationEventTopics.PAYMENT_SUCCESS, groupId="${spring.kafka.consumer.group-id}")
    public void onSuccess(ConsumerRecord<String, PaymentSuccessEvent> record) { inboxService.receive(record, consumerGroup); }
    @KafkaListener(topics=NotificationEventTopics.PAYMENT_FAILED, groupId="${spring.kafka.consumer.group-id}")
    public void onFailed(ConsumerRecord<String, PaymentFailedEvent> record) { inboxService.receive(record, consumerGroup); }
}
