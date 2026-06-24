package com.project.common.events.field;

import java.math.BigDecimal;
import java.time.LocalTime;

public record TimePriceRuleSnapshot(
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal hourlyPrice
) {
}
