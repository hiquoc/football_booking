package com.project.booking.community.kafka;

import com.project.booking.community.entity.MatchEvaluation;
import com.project.common.events.notification.MatchEvaluationSubmittedEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.outbox.dto.OutboxSaveRequest;
import com.project.common.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class MatchEvaluationEventPublisher {
    private final OutboxService outboxService;

    public void publish(MatchEvaluation evaluation) {
        MatchEvaluationSubmittedEvent event = new MatchEvaluationSubmittedEvent(
                evaluation.getId(),
                evaluation.getPostId(),
                evaluation.getBookingId(),
                evaluation.getEvaluatorId(),
                evaluation.getEvaluatedUserId(),
                Boolean.TRUE.equals(evaluation.getArrivedOnTime()),
                Boolean.TRUE.equals(evaluation.getCancelledUnexpectedly()),
                Boolean.TRUE.equals(evaluation.getFairPlay()),
                Boolean.TRUE.equals(evaluation.getWouldPlayAgain()),
                evaluation.getComment(),
                evaluation.getCreatedAt() == null ? Instant.now() : evaluation.getCreatedAt().toInstant(ZoneOffset.UTC));
        outboxService.save(new OutboxSaveRequest(
                "MatchEvaluation",
                evaluation.getId().toString(),
                event.getClass().getSimpleName(),
                NotificationEventTopics.MATCH_EVALUATION_SUBMITTED,
                evaluation.getEvaluatedUserId().toString(),
                event));
    }
}
