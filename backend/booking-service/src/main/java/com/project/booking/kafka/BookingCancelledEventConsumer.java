package com.project.booking.kafka;

import com.project.common.events.notification.BookingCancelledEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingCancelledEventConsumer {
    private final InboxService inboxService;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @KafkaListener(topics = NotificationEventTopics.BOOKING_CANCELLED, groupId = "${spring.kafka.consumer.group-id}")
    public void onBookingCancelled(ConsumerRecord<String, BookingCancelledEvent> record) {
        inboxService.receive(record, consumerGroup);
    }
}
