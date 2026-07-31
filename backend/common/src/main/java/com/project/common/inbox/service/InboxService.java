package com.project.common.inbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.kafka.KafkaHeaderUtil;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.entity.InboxEventStatus;
import com.project.common.inbox.repository.InboxEventRepository;
import com.project.common.logging.LogContext;
import com.project.common.logging.MdcFields;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InboxService {

    private final InboxEventRepository inboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void receive(ConsumerRecord<String, ?> record, String consumerGroup) {
        Map<String, String> previousContext = org.slf4j.MDC.getCopyOfContextMap();
        try {
            String eventId = eventId(record);
            String eventType = KafkaHeaderUtil.header(record, KafkaHeaderUtil.EVENT_TYPE)
                    .orElse(payloadEventType(record.value()));
            String requestId = KafkaHeaderUtil.header(record, KafkaHeaderUtil.REQUEST_ID).orElse(null);
            String aggregateId = KafkaHeaderUtil.header(record, KafkaHeaderUtil.AGGREGATE_ID).orElse(record.key());
            LogContext.putIfPresent(MdcFields.REQUEST_ID, requestId);
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
                        "kafka_consumer_received eventId={} topic={} partition={} offset={} key={} eventType={} requestId={} aggregateId={} consumerGroup={}",
                        eventId,
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        record.key(),
                        eventType,
                        requestId,
                        aggregateId,
                        consumerGroup);
            } catch (DataIntegrityViolationException ignored) {
                // Another instance already inserted the same event for this consumer group.
            }
        } finally {
            LogContext.restore(previousContext);
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

    private String payloadEventType(Object payload) {
        return payload == null ? null : payload.getClass().getSimpleName();
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize inbox event", ex);
        }
    }
}
