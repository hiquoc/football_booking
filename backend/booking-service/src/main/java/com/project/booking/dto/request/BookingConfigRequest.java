package com.project.booking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingConfigRequest(
        @NotNull @Min(0) Long bookingFee,
        @NotNull @Min(0) Integer refundBeforeHours,
        @NotNull Boolean refundEnabled
) {
}
