package com.project.booking.moderation.controller;

import com.project.booking.moderation.dto.*;
import com.project.booking.moderation.enums.PaymentDisputeStatus;
import com.project.booking.moderation.service.BookingModerationService;
import com.project.common.dto.ApiResponse;
import com.project.common.dto.PageResponse;
import com.project.common.security.CurrentUser;
import com.project.common.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/moderation")
@RequiredArgsConstructor
public class BookingModerationController {
    private final BookingModerationService service;

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/owner/no-shows")
    public ResponseEntity<ApiResponse<FieldViolationResponse>> reportNoShow(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody ReportNoShowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("No-show reported", service.reportNoShow(user.id(), request)));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PageableAsQueryParam
    @GetMapping("/owner/fields/{fieldId}/violations")
    public ResponseEntity<ApiResponse<PageResponse<FieldViolationResponse>>> violations(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID fieldId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.getViolations(user.id(), fieldId, pageable)));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PageableAsQueryParam
    @GetMapping("/owner/fields/{fieldId}/banned-clients")
    public ResponseEntity<ApiResponse<PageResponse<FieldViolationResponse>>> bannedClients(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID fieldId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.getBannedClients(user.id(), fieldId, pageable)));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/owner/fields/{fieldId}/banned-clients/{userId}/unban")
    public ResponseEntity<ApiResponse<FieldViolationResponse>> unban(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID fieldId,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success("Client unbanned", service.unban(user.id(), fieldId, userId)));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/owner/payment-disputes")
    public ResponseEntity<ApiResponse<PaymentDisputeReportResponse>> createDispute(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody CreatePaymentDisputeReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment dispute submitted", service.createPaymentDispute(user.id(), request)));
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
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.getAdminDisputes(status, pageable)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/payment-disputes/{reportId}/review")
    public ResponseEntity<ApiResponse<PaymentDisputeReportResponse>> reviewDispute(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID reportId,
            @Valid @RequestBody ReviewPaymentDisputeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment dispute reviewed",
                service.reviewPaymentDispute(user.id(), reportId, request)));
    }
}
