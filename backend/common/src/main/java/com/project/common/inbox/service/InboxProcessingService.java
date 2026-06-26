package com.project.common.inbox.service;

import com.project.common.inbox.config.InboxProperties;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.entity.InboxEventStatus;
import com.project.common.inbox.handler.InboxEventHandler;
import com.project.common.inbox.repository.InboxEventRepository;
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
public class InboxProcessingService {

    private final InboxEventRepository inboxEventRepository;
    private final List<InboxEventHandler> handlers;
    private final InboxProperties properties;

    @Transactional
    public List<InboxEvent> claimBatch() {
        List<InboxEvent> events = inboxEventRepository.lockReceivedBatch(properties.batchSize());
        events.forEach(event -> event.setStatus(InboxEventStatus.PROCESSING));
        return inboxEventRepository.saveAll(events);
    }

    public void handle(InboxEvent event) {
        InboxEventHandler handler = handlers.stream()
                .filter(candidate -> candidate.supports(event.getTopic()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No inbox handler for topic " + event.getTopic()));
        handler.handle(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(UUID eventId) {
        InboxEvent event = inboxEventRepository.findById(eventId).orElseThrow();
        event.setStatus(InboxEventStatus.PROCESSED);
        event.setProcessedAt(Instant.now());
        event.setErrorMessage(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID eventId, String errorMessage) {
        InboxEvent event = inboxEventRepository.findById(eventId).orElseThrow();
        event.setRetryCount(event.getRetryCount() + 1);
        event.setErrorMessage(errorMessage);
        if (event.getRetryCount() >= properties.maxRetries()) {
            event.setStatus(InboxEventStatus.FAILED);
            return;
        }
        event.setStatus(InboxEventStatus.RECEIVED);
        event.setNextRetryAt(Instant.now().plus(retryDelay(event.getRetryCount())));
    }

    @Transactional
    public void replayFailed(UUID eventId) {
        InboxEvent event = inboxEventRepository.findById(eventId).orElseThrow();
        if (event.getStatus() != InboxEventStatus.FAILED) {
            throw new IllegalStateException("Only FAILED inbox events can be replayed");
        }
        event.setStatus(InboxEventStatus.RECEIVED);
        event.setRetryCount(0);
        event.setNextRetryAt(Instant.now());
        event.setProcessedAt(null);
        event.setErrorMessage(null);
    }

    private Duration retryDelay(int retryCount) {
        int index = Math.max(0, Math.min(retryCount - 1, properties.retryDelays().size() - 1));
        return properties.retryDelays().get(index);
    }
}
