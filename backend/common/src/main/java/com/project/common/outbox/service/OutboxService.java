package com.project.common.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.kafka.KafkaHeaderUtil;
import com.project.common.outbox.dto.OutboxSaveRequest;
import com.project.common.outbox.entity.OutboxEvent;
import com.project.common.outbox.entity.OutboxEventStatus;
import com.project.common.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private static final TypeReference<Map<String, String>> HEADERS = new TypeReference<>() {
    };

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OutboxEvent save(OutboxSaveRequest request) {
        UUID eventId = UUID.randomUUID();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(KafkaHeaderUtil.EVENT_ID, eventId.toString());
        headers.put("aggregateType", request.aggregateType());
        headers.put("eventType", request.eventType());

        OutboxEvent event = OutboxEvent.builder()
                .id(eventId)
                .aggregateType(request.aggregateType())
                .aggregateId(request.aggregateId())
                .eventType(request.eventType())
                .topic(request.topic())
                .eventKey(request.eventKey())
                .payload(write(request.payload()))
                .headers(write(headers))
                .status(OutboxEventStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(Instant.now())
                .build();
        return outboxEventRepository.save(event);
    }

    public Object payload(OutboxEvent event) {
        try {
            return objectMapper.readValue(event.getPayload(), Object.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize outbox payload " + event.getId(), ex);
        }
    }

    public Map<String, String> headers(OutboxEvent event) {
        try {
            return objectMapper.readValue(event.getHeaders(), HEADERS);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize outbox headers " + event.getId(), ex);
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize outbox event", ex);
        }
    }
}
