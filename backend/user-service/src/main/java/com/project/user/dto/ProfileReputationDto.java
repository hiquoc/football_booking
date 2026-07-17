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
public class ProfileReputationDto {
    private BigDecimal noCancelRate;
    private BigDecimal onTimeRate;
    private BigDecimal fairPlayRate;
}
