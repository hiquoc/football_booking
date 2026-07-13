package com.project.payment.kafka;
import com.project.common.events.notification.*;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class BookingCreatedConsumer {
    private final InboxService inboxService;
    @Value("${spring.kafka.consumer.group-id}") private String consumerGroup;
    @KafkaListener(topics=NotificationEventTopics.BOOKING_CREATED, groupId="${spring.kafka.consumer.group-id}")
    public void onBookingCreated(ConsumerRecord<String, BookingCreatedEvent> record) { inboxService.receive(record, consumerGroup); }
}
