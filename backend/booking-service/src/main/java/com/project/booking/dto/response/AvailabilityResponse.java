package com.project.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {
    private LocalTime openTime;
    private LocalTime closeTime;
    private Boolean open24Hours;
    private List<AvailabilityOperatingHoursResponse> operatingHours;
    private List<UnavailableSlotResponse> unavailableSlots;
}
