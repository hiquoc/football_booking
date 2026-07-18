package com.project.booking.community.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CommunityPlayerStatisticsResponse {
    private Integer totalMatches;
    private BigDecimal winRate;
    private BigDecimal onTimeRate;
    private BigDecimal noCancelRate;
    private BigDecimal fairPlayRate;
    private Integer completedBookingCount;
}
