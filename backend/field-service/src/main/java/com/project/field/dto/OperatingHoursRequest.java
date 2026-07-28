package com.project.field.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatingHoursRequest {
    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    private LocalTime openTime;

    private LocalTime closeTime;

    @Builder.Default
    private Boolean closed = false;

    @Builder.Default
    private Boolean open24Hours = false;
}
