package com.project.booking.dto.response;

import com.project.common.enums.RecurringBookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringBookingResponse {
    private UUID id;
    private UUID userId;
    private UUID fieldId;
    private String fieldName;
    private UUID subFieldId;
    private String subFieldName;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate startDate;
    private LocalDate endDate;
    private RecurringBookingStatus status;
    private LocalDateTime nextProcessAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
