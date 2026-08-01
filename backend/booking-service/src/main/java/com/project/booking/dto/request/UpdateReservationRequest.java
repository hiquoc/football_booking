package com.project.booking.dto.request;

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
public class UpdateReservationRequest {

    @NotNull(message = "Reservation ID is required")
    private UUID reservationId;

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

    public CreateBookingRequest toCreateBookingRequest() {
        return CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(bookingDate)
                .startTime(startTime)
                .durationMinutes(durationMinutes)
                .endTime(endTime)
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .note(note)
                .build();
    }
}
