package com.project.booking.moderation.service;

import com.project.booking.moderation.dto.*;
import com.project.booking.moderation.enums.PaymentDisputeStatus;
import com.project.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BookingModerationService {
    FieldViolationResponse reportNoShow(UUID ownerId, ReportNoShowRequest request);
    PageResponse<FieldViolationResponse> getViolations(UUID ownerId, UUID fieldId, Pageable pageable);
    PageResponse<FieldViolationResponse> getBannedClients(UUID ownerId, UUID fieldId, Pageable pageable);
    FieldViolationResponse unban(UUID ownerId, UUID fieldId, UUID userId);
    int recoverMonthlyViolations();
    void ensureCanBook(UUID userId, UUID fieldId);
    void ensurePlatformAllowed(UUID userId);
    PaymentDisputeReportResponse createPaymentDispute(UUID ownerId, CreatePaymentDisputeReportRequest request);
    PageResponse<PaymentDisputeReportResponse> getOwnerDisputes(UUID ownerId, Pageable pageable);
    PageResponse<PaymentDisputeReportResponse> getAdminDisputes(PaymentDisputeStatus status, Pageable pageable);
    PaymentDisputeReportResponse reviewPaymentDispute(UUID adminId, UUID reportId, ReviewPaymentDisputeRequest request);
}
