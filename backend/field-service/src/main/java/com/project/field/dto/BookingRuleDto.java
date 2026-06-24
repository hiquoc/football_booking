package com.project.field.dto;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRuleDto {
    private Long id;

    @Positive(message = "Minimum booking duration must be greater than 0")
    private Integer minimumBookingDurationMinutes;

    @Positive(message = "Maximum booking duration must be greater than 0")
    private Integer maximumBookingDurationMinutes;

    @Positive(message = "Booking interval must be greater than 0")
    private Integer bookingIntervalMinutes;
}