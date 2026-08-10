package com.project.common.events.notification;

import java.time.Instant;
import java.util.UUID;

public record MatchEvaluationSubmittedEvent(
        UUID evaluationId,
        UUID postId,
        UUID bookingId,
        UUID evaluatorId,
        UUID evaluatedUserId,
        boolean arrivedOnTime,
        boolean cancelledUnexpectedly,
        boolean fairPlay,
        boolean wouldPlayAgain,
        String skillLevel,
        String comment,
        Instant occurredAt
) {
}
