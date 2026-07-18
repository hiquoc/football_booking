package com.project.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileStatisticsDto {
    private int totalMatches;
    private int wins;
    private int draws;
    private int losses;
    private BigDecimal winRate;
    private int completedBookingCount;
}
