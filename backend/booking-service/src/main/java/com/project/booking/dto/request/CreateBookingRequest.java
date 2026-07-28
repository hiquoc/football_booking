package com.project.booking.dto.request;

import com.project.common.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotNull(message = "Sub-field ID is required")
    private UUID subFieldId;

    private LocalDate bookingDate;

    private LocalTime startTime;

    @Positive(message = "Duration must be greater than 0")
    @NotNull(message = "Duration is required")
    private Integer durationMinutes;

    @Schema(hidden = true)
    private LocalTime endTime;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private String note;

    @Builder.Default
    @Schema(defaultValue = "ACCOUNT_BALANCE")
    private PaymentMethod paymentMethod = PaymentMethod.ACCOUNT_BALANCE;
}
