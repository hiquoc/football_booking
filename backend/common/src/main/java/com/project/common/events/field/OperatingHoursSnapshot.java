package com.project.common.events.field;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record OperatingHoursSnapshot(
        DayOfWeek dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        Boolean closed,
        Boolean open24Hours
) {
}
