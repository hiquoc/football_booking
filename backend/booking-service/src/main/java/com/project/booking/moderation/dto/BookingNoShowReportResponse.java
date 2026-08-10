package com.project.booking.moderation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingNoShowReportResponse {
    private UUID id;
    private UUID bookingId;
    private UUID fieldId;
    private UUID reportedUserId;
    private String reportedUsername;
    private String reportedPhoneNumber;
    private UUID ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
