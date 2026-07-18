package com.project.booking.dto.response;

import com.project.booking.enums.WinningTeam;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MatchResultResponse {
    private UUID id;
    private UUID bookingId;
    private WinningTeam winningTeam;
    private Integer teamAPercentage;
    private Integer teamBPercentage;
    private BigDecimal teamAAmount;
    private BigDecimal teamBAmount;
    private UUID submittedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
