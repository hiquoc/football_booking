package com.project.common.outbox.service;

import com.project.common.kafka.KafkaProducerService;
import com.project.common.kafka.KafkaTopics;
import com.project.common.outbox.config.OutboxProperties;
import com.project.common.outbox.dto.OutboxDlqPayload;
import com.project.common.outbox.entity.OutboxEvent;
import com.project.common.outbox.entity.OutboxEventStatus;
import com.project.common.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxProcessingService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaProducerService kafkaProducerService;
    private final OutboxService outboxService;
    private final OutboxProperties properties;

    @Transactional
    public List<OutboxEvent> claimBatch() {
        List<OutboxEvent> events = outboxEventRepository.lockPendingBatch(properties.batchSize());
        events.forEach(event -> event.setStatus(OutboxEventStatus.PROCESSING));
        return outboxEventRepository.saveAll(events);
    }

    public void publish(OutboxEvent event) {
        kafkaProducerService.publish(
                event.getTopic(),
                event.getEventKey(),
                outboxService.payload(event),
                outboxService.headers(event));
    }

    public void publishDlq(OutboxEvent event, String errorMessage) {
        if (!properties.dlq().enabled()) {
            return;
        }
        kafkaProducerService.publishDlq(
                KafkaTopics.dlqTopic(event.getTopic(), properties.dlq().topicSuffix()),
                event.getEventKey(),
                new OutboxDlqPayload(
                        event.getId(),
                        event.getAggregateId(),
                        event.getAggregateType(),
                        event.getEventType(),
                        event.getTopic(),
                        event.getPayload(),
                        event.getRetryCount(),
                        Instant.now(),
                        errorMessage),
                outboxService.headers(event));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(UUID eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId).orElseThrow();
        event.setStatus(OutboxEventStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
        event.setErrorMessage(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDeadLetter(UUID eventId, String errorMessage) {
        OutboxEvent event = outboxEventRepository.findById(eventId).orElseThrow();
        event.setStatus(OutboxEventStatus.DEAD_LETTER);
        event.setErrorMessage(errorMessage);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markFailed(UUID eventId, String errorMessage) {
        OutboxEvent event = outboxEventRepository.findById(eventId).orElseThrow();
        event.setRetryCount(event.getRetryCount() + 1);
        event.setErrorMessage(errorMessage);
        if (event.getRetryCount() >= properties.maxRetries()) {
            event.setStatus(OutboxEventStatus.PROCESSING);
            return true;
        }
        event.setStatus(OutboxEventStatus.PENDING);
        event.setNextRetryAt(Instant.now().plus(retryDelay(event.getRetryCount())));
        return false;
    }

    @Transactional
    public void replayDeadLetter(UUID eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId).orElseThrow();
        if (event.getStatus() != OutboxEventStatus.DEAD_LETTER) {
            throw new IllegalStateException("Only DEAD_LETTER outbox events can be replayed");
        }
        event.setStatus(OutboxEventStatus.PENDING);
        event.setRetryCount(0);
        event.setNextRetryAt(Instant.now());
        event.setErrorMessage(null);
        event.setPublishedAt(null);
    }

    private Duration retryDelay(int retryCount) {
        int index = Math.max(0, Math.min(retryCount - 1, properties.retryDelays().size() - 1));
        return properties.retryDelays().get(index);
    }
}
