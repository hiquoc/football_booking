package com.project.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityOperatingHoursResponse {
    private LocalDate date;
    private LocalTime openTime;
    private LocalTime closeTime;
    private Boolean closed;
    private Boolean open24Hours;
}
