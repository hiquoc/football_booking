package com.project.booking.kafka;

import com.project.common.events.field.*;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FieldProjectionConsumer {

    private final InboxService inboxService;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @KafkaListener(topics = FieldEventTopics.SUB_FIELD_CREATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onSubFieldCreated(ConsumerRecord<String, SubFieldCreatedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }

    @KafkaListener(topics = FieldEventTopics.SUB_FIELD_UPDATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onSubFieldUpdated(ConsumerRecord<String, SubFieldUpdatedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }

    @KafkaListener(topics = FieldEventTopics.SUB_FIELD_DELETED, groupId = "${spring.kafka.consumer.group-id}")
    public void onSubFieldDeleted(ConsumerRecord<String, SubFieldDeletedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }

    @KafkaListener(topics = FieldEventTopics.FIELD_OPERATING_HOURS_UPDATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onFieldOperatingHoursUpdated(ConsumerRecord<String, FieldOperatingHoursUpdatedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }

    @KafkaListener(topics = FieldEventTopics.SUB_FIELD_OPERATING_HOURS_UPDATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onSubFieldOperatingHoursUpdated(ConsumerRecord<String, SubFieldOperatingHoursUpdatedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }

    @KafkaListener(topics = FieldEventTopics.FIELD_CLOSURE_CREATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onClosureCreated(ConsumerRecord<String, FieldClosureCreatedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }

    @KafkaListener(topics = FieldEventTopics.FIELD_CLOSURE_UPDATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onClosureUpdated(ConsumerRecord<String, FieldClosureUpdatedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }

    @KafkaListener(topics = FieldEventTopics.FIELD_CLOSURE_DELETED, groupId = "${spring.kafka.consumer.group-id}")
    public void onClosureDeleted(ConsumerRecord<String, FieldClosureDeletedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }
}
