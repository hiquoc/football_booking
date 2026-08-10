package com.project.booking.moderation.dto;

import com.project.booking.moderation.enums.PaymentDisputeStatus;
import com.project.common.enums.BookingPaymentStatus;
import com.project.common.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private String fieldName;
    private UUID subFieldId;
    private String subFieldName;
    private UUID reportedUserId;
    private String reportedUsername;
    private String reportedPhoneNumber;
    private String reportedUserStatus;
    private UUID ownerId;
    private String bookingCode;
    private LocalDate bookingDate;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long bookingPrice;
    private Long platformBookingFee;
    private BookingStatus bookingStatus;
    private BookingPaymentStatus bookingPaymentStatus;
    private String description;
    private PaymentDisputeStatus status;
    private String adminNote;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private UUID reviewedBy;
}
