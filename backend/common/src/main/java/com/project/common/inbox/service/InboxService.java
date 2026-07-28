package com.project.common.inbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.kafka.KafkaHeaderUtil;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.entity.InboxEventStatus;
import com.project.common.inbox.repository.InboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class InboxService {

    private final InboxEventRepository inboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void receive(ConsumerRecord<String, ?> record, String consumerGroup) {
        String eventId = eventId(record);
        if (inboxEventRepository.findByEventIdAndConsumerGroup(eventId, consumerGroup).isPresent()) {
            return;
        }
        Object payload = record.value();
        InboxEvent event = InboxEvent.builder()
                .eventId(eventId)
                .consumerGroup(consumerGroup)
                .topic(record.topic())
                .partition(record.partition())
                .offset(record.offset())
                .payloadType(payload.getClass().getName())
                .payload(write(payload))
                .status(InboxEventStatus.RECEIVED)
                .retryCount(0)
                .nextRetryAt(Instant.now())
                .build();
        try {
            inboxEventRepository.saveAndFlush(event);
            log.info(
                    "Received Kafka event: eventId={}, topic={}, partition={}, offset={}, key={}, consumerGroup={}",
                    eventId,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    consumerGroup);
        } catch (DataIntegrityViolationException ignored) {
            // Another instance already inserted the same event for this consumer group.
        }
    }

    public <T> T payload(InboxEvent event, Class<T> type) {
        try {
            return objectMapper.readValue(event.getPayload(), type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize inbox payload " + event.getId(), ex);
        }
    }

    private String eventId(ConsumerRecord<String, ?> record) {
        Header header = record.headers().lastHeader(KafkaHeaderUtil.EVENT_ID);
        if (header != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return record.topic() + "-" + record.partition() + "-" + record.offset();
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize inbox event", ex);
        }
    }
}
