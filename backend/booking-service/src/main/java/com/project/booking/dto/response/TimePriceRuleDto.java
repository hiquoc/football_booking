package com.project.booking.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimePriceRuleDto {
    private Long id;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal hourlyPrice;
}
