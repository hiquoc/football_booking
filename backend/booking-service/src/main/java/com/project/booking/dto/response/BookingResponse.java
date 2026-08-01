package com.project.booking.dto.response;

import com.project.common.enums.BookingCancelledBy;
import com.project.common.enums.BookingPaymentStatus;
import com.project.common.enums.BookingStatus;
import com.project.common.enums.BookingType;
import com.project.common.enums.PaymentMethod;
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
    private String clientName;
    private String clientPhoneNumber;
    private String clientAvatarUrl;
    private UUID subFieldId;
    private String subFieldName;
    private UUID fieldId;
    private String fieldName;
    private UUID ownerId;
    private LocalDate bookingDate;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer durationMinutes;
    private BigDecimal pricePerHour;
    private BigDecimal subFieldPrice;
    private Long bookingPrice;
    private Long platformBookingFee;
    private BookingType bookingType;
    private PaymentMethod paymentMethod;
    private BookingStatus status;
    private BookingPaymentStatus paymentStatus;
    private String note;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private LocalDateTime paymentExpiresAt;
    private BookingCancelledBy cancelledBy;
    private MatchResultResponse matchResult;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
