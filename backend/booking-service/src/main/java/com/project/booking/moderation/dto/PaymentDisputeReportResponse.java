package com.project.booking.moderation.dto;

import com.project.booking.moderation.enums.PaymentDisputeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDisputeReportResponse {
    private UUID id;
    private UUID bookingId;
    private UUID fieldId;
    private UUID reportedUserId;
    private UUID ownerId;
    private String description;
    private PaymentDisputeStatus status;
    private String adminNote;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private UUID reviewedBy;
}
