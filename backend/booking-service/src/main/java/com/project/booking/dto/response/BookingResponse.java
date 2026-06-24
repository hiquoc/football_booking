package com.project.booking.dto.response;

import com.project.common.enums.BookingCancelledBy;
import com.project.common.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private UUID id;
    private String bookingCode;
    private UUID clientId;
    private UUID subFieldId;
    private String subFieldName;
    private String fieldName;
    private UUID ownerId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private BigDecimal pricePerHour;
    private BigDecimal totalAmount;
    private BookingStatus status;
    private String note;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private BookingCancelledBy cancelledBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
