package com.project.booking.moderation.service;

import com.project.booking.moderation.dto.*;
import com.project.booking.moderation.enums.PaymentDisputeStatus;
import com.project.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BookingModerationService {
    FieldViolationResponse reportNoShow(UUID actorId, String actorRole, ReportNoShowRequest request);
    PageResponse<FieldViolationResponse> getUserFieldViolations(UUID userId, Pageable pageable);
    PageResponse<FieldViolationResponse> getViolations(UUID actorId, String actorRole, UUID fieldId, Pageable pageable);
    PageResponse<FieldViolationResponse> getBannedClients(UUID actorId, String actorRole, UUID fieldId, Pageable pageable);
    FieldViolationResponse ban(UUID actorId, String actorRole, UUID fieldId, UUID userId);
    FieldViolationResponse unban(UUID actorId, String actorRole, UUID fieldId, UUID userId);
    int recoverMonthlyViolations();
    void ensureCanBook(UUID userId, UUID fieldId);
    void ensurePlatformAllowed(UUID userId);
    PaymentDisputeReportResponse createPaymentDispute(UUID ownerId, CreatePaymentDisputeReportRequest request);
    PageResponse<PaymentDisputeReportResponse> getOwnerDisputes(UUID ownerId, Pageable pageable);
    PageResponse<PaymentDisputeReportResponse> getAdminDisputes(PaymentDisputeStatus status, Pageable pageable);
    PaymentDisputeReportResponse reviewPaymentDispute(UUID adminId, UUID reportId, ReviewPaymentDisputeRequest request);
}
