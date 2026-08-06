package com.project.booking.kafka;

import com.project.booking.events.RecurringBookingEventTopics;
import com.project.booking.events.RecurringBookingOccurrenceRequestedEvent;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecurringBookingOccurrenceEventConsumer {

    private final InboxService inboxService;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @KafkaListener(topics = RecurringBookingEventTopics.RECURRING_OCCURRENCE_REQUESTED, groupId = "${spring.kafka.consumer.group-id}")
    public void onOccurrenceRequested(ConsumerRecord<String, RecurringBookingOccurrenceRequestedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }
}
