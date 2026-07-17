package com.project.booking.community.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MatchEvaluationResponse {
    private UUID id;
    private UUID postId;
    private UUID bookingId;
    private UUID evaluatorId;
    private UUID evaluatedUserId;
    private Boolean arrivedOnTime;
    private Boolean cancelledUnexpectedly;
    private Boolean fairPlay;
    private Boolean wouldPlayAgain;
    private String comment;
    private LocalDateTime createdAt;
}
