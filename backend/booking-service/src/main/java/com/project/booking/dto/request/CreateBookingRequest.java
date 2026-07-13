package com.project.booking.dto.request;

import com.project.common.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotNull(message = "Sub-field ID is required")
    private UUID subFieldId;

    @NotNull(message = "Booking date is required")
    @FutureOrPresent(message = "Booking date must be today or in the future")
    private LocalDate bookingDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @Positive(message = "Duration must be greater than 0")
    @NotNull(message = "Duration is required")
    private Integer durationMinutes;

    @Schema(hidden = true)
    private LocalTime endTime;

    private String note;

    @Builder.Default
    @Schema(defaultValue = "STRIPE")
    private PaymentMethod paymentMethod = PaymentMethod.STRIPE;
}
