package com.project.booking.moderation.controller;

import com.project.booking.moderation.dto.*;
import com.project.booking.moderation.enums.PaymentDisputeStatus;
import com.project.booking.moderation.service.BookingModerationService;
import com.project.common.dto.ApiResponse;
import com.project.common.dto.PageResponse;
import com.project.common.enums.ApiStatusCode;
import com.project.common.security.CurrentUser;
import com.project.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/moderation")
@RequiredArgsConstructor
public class BookingModerationController {
    private final BookingModerationService service;

    @PreAuthorize("hasAnyRole('OWNER','EMPLOYEE')")
    @PostMapping("/owner/no-shows")
    public ResponseEntity<ApiResponse<FieldViolationResponse>> reportNoShow(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody ReportNoShowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ApiStatusCode.NO_SHOW_REPORTED, "No-show reported", service.reportNoShow(user.id(), user.role(), request)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PageableAsQueryParam
    @GetMapping("/admin/users/{userId}/field-violations")
    public ResponseEntity<ApiResponse<PageResponse<FieldViolationResponse>>> userFieldViolations(
            @PathVariable UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.getUserFieldViolations(userId, pageable)));
    }

    @PreAuthorize("hasAnyRole('OWNER','EMPLOYEE')")
    @PageableAsQueryParam
    @Operation(summary = "List field violations", description = "Returns paged field violations enriched with username, phoneNumber, and userId for authorized field managers.")
    @GetMapping("/owner/fields/{fieldId}/violations")
    public ResponseEntity<ApiResponse<PageResponse<FieldViolationResponse>>> violations(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID fieldId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.getViolations(user.id(), user.role(), fieldId, pageable)));
    }

    @PreAuthorize("hasAnyRole('OWNER','EMPLOYEE')")
    @PageableAsQueryParam
    @Operation(summary = "List banned clients for a field", description = "Returns paged banned clients enriched with username, phoneNumber, and userId for authorized field managers.")
    @GetMapping("/owner/fields/{fieldId}/banned-clients")
    public ResponseEntity<ApiResponse<PageResponse<FieldViolationResponse>>> bannedClients(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID fieldId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.getBannedClients(user.id(), user.role(), fieldId, pageable)));
    }

    @PreAuthorize("hasAnyRole('OWNER','EMPLOYEE')")
    @PageableAsQueryParam
    @GetMapping("/owner/fields/{fieldId}/no-show-reports")
    public ResponseEntity<ApiResponse<PageResponse<BookingNoShowReportResponse>>> noShowReports(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID fieldId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.getNoShowReports(user.id(), user.role(), fieldId, pageable)));
    }

    @PreAuthorize("hasAnyRole('OWNER','EMPLOYEE')")
    @PageableAsQueryParam
    @GetMapping("/owner/fields/{fieldId}/audit-logs")
    public ResponseEntity<ApiResponse<PageResponse<ModerationAuditLogResponse>>> auditLogs(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID fieldId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.getAuditLogs(user.id(), user.role(), fieldId, pageable)));
    }

    @PreAuthorize("hasAnyRole('OWNER','EMPLOYEE')")
    @PatchMapping("/owner/fields/{fieldId}/banned-clients/{userId}/ban")
    public ResponseEntity<ApiResponse<FieldViolationResponse>> ban(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID fieldId,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(ApiStatusCode.USER_BANNED, "Client banned", service.ban(user.id(), user.role(), fieldId, userId)));
    }

    @PreAuthorize("hasAnyRole('OWNER','EMPLOYEE')")
    @PatchMapping("/owner/fields/{fieldId}/banned-clients/{userId}/unban")
    public ResponseEntity<ApiResponse<FieldViolationResponse>> unban(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID fieldId,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(ApiStatusCode.USER_UNBANNED, "Client unbanned", service.unban(user.id(), user.role(), fieldId, userId)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PageableAsQueryParam
    @GetMapping("/admin/users/{userId}/audit-logs")
    public ResponseEntity<ApiResponse<PageResponse<ModerationAuditLogResponse>>> userAuditLogs(
            @PathVariable UUID userId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.getUserAuditLogs(userId, pageable)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/users/{userId}/platform-ban/reset")
    public ResponseEntity<ApiResponse<ModerationResetResponse>> resetPlatformBan(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(ApiStatusCode.USER_UNBANNED, "Platform ban reset", service.resetPlatformBan(user.id(), userId)));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/owner/payment-disputes")
    public ResponseEntity<ApiResponse<PaymentDisputeReportResponse>> createDispute(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody CreatePaymentDisputeReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ApiStatusCode.PAYMENT_DISPUTE_REPORTED, "Payment dispute submitted", service.createPaymentDispute(user.id(), request)));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PageableAsQueryParam
    @GetMapping("/owner/payment-disputes")
    public ResponseEntity<ApiResponse<PageResponse<PaymentDisputeReportResponse>>> ownerDisputes(
            @CurrentUser UserPrincipal user,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.getOwnerDisputes(user.id(), pageable)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PageableAsQueryParam
    @GetMapping("/admin/payment-disputes")
    public ResponseEntity<ApiResponse<PageResponse<PaymentDisputeReportResponse>>> adminDisputes(
            @RequestParam(required = false) PaymentDisputeStatus status,
            @RequestParam(required = false) List<UUID> fieldIds,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.getAdminDisputes(status, fieldIds, pageable)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/payment-disputes/{reportId}/review")
    public ResponseEntity<ApiResponse<PaymentDisputeReportResponse>> reviewDispute(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID reportId,
            @Valid @RequestBody ReviewPaymentDisputeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ApiStatusCode.PAYMENT_DISPUTE_REVIEWED, "Payment dispute reviewed",
                service.reviewPaymentDispute(user.id(), reportId, request)));
    }
}
