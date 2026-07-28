package com.project.field.dto;

import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatingHoursDto {
    private UUID id;
    private UUID fieldId;
    private UUID subFieldId;
    private DayOfWeek dayOfWeek;
    private LocalTime openTime;
    private LocalTime closeTime;
    private Boolean closed;
    private Boolean open24Hours;
}
