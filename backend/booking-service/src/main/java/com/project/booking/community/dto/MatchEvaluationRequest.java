package com.project.booking.community.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class MatchEvaluationRequest {
    @NotNull
    private UUID evaluatedUserId;
    @NotNull
    private Boolean arrivedOnTime;
    @NotNull
    private Boolean cancelledUnexpectedly;
    @NotNull
    private Boolean fairPlay;
    @NotNull
    private Boolean wouldPlayAgain;
    @Size(max = 1000)
    private String comment;
}
