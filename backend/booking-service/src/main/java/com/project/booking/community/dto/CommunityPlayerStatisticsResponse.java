package com.project.booking.community.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPlayerStatisticsResponse {
    private Integer totalMatches;
    private BigDecimal winRate;
    private BigDecimal onTimeRate;
    private BigDecimal noCancelRate;
    private BigDecimal fairPlayRate;
    private Integer completedBookingCount;
}
