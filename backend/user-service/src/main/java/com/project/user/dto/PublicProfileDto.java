package com.project.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicProfileDto {
    private ProfileInfoDto personal;
    private ProfileStatisticsDto statistics;
    private ProfileReputationDto reputation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
