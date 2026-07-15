package com.project.booking.controller;

import com.project.booking.dto.request.CreateRecurringBookingRequest;
import com.project.booking.dto.request.UpdateRecurringBookingRequest;
import com.project.booking.dto.response.RecurringBookingResponse;
import com.project.booking.service.RecurringBookingService;
import com.project.common.dto.ApiResponse;
import com.project.common.dto.PageResponse;
import com.project.common.enums.RecurringBookingStatus;
import com.project.common.security.CurrentUser;
import com.project.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/recurring-bookings")
@RequiredArgsConstructor
@Tag(name = "Recurring Bookings", description = "Recurring weekly booking rules")
public class RecurringBookingController {

    private final RecurringBookingService recurringBookingService;

    @Operation(summary = "Create recurring booking")
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping
    public ResponseEntity<ApiResponse<RecurringBookingResponse>> create(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody CreateRecurringBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Recurring booking created successfully",
                        recurringBookingService.create(user.id(), request)));
    }

    @Operation(summary = "Get my recurring bookings")
    @PreAuthorize("hasRole('CLIENT')")
    @PageableAsQueryParam
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<RecurringBookingResponse>>> getMine(
            @Parameter(hidden = true) @CurrentUser UserPrincipal user,
            @RequestParam(required = false) RecurringBookingStatus status,
            @Parameter(hidden = true) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(recurringBookingService.getMine(user.id(), status, pageable)));
    }

    @Operation(summary = "Get recurring bookings for owned fields")
    @PreAuthorize("hasRole('OWNER')")
    @PageableAsQueryParam
    @GetMapping("/owner")
    public ResponseEntity<ApiResponse<PageResponse<RecurringBookingResponse>>> getForOwner(
            @Parameter(hidden = true) @CurrentUser UserPrincipal user,
            @RequestParam(required = false) RecurringBookingStatus status,
            @Parameter(hidden = true) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(recurringBookingService.getForOwner(user.id(), status, pageable)));
    }

    @Operation(summary = "Admin list recurring bookings")
    @PreAuthorize("hasRole('ADMIN')")
    @PageableAsQueryParam
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<PageResponse<RecurringBookingResponse>>> getForAdmin(
            @RequestParam(required = false) RecurringBookingStatus status,
            @Parameter(hidden = true) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(recurringBookingService.getForAdmin(status, pageable)));
    }

    @Operation(summary = "Edit recurring booking")
    @PreAuthorize("hasRole('CLIENT')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringBookingResponse>> update(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecurringBookingRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Recurring booking updated successfully",
                recurringBookingService.update(user.id(), id, request)));
    }

    @Operation(summary = "Pause recurring booking")
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/{id}/pause")
    public ResponseEntity<ApiResponse<RecurringBookingResponse>> pause(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Recurring booking paused successfully",
                recurringBookingService.pause(user.id(), id)));
    }

    @Operation(summary = "Resume recurring booking")
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/{id}/resume")
    public ResponseEntity<ApiResponse<RecurringBookingResponse>> resume(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Recurring booking resumed successfully",
                recurringBookingService.resume(user.id(), id)));
    }

    @Operation(summary = "Cancel recurring booking")
    @PreAuthorize("hasRole('CLIENT')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringBookingResponse>> cancel(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Recurring booking cancelled successfully",
                recurringBookingService.cancel(user.id(), id)));
    }

    @Operation(summary = "Admin pause recurring booking")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/{id}/pause")
    public ResponseEntity<ApiResponse<RecurringBookingResponse>> adminPause(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(recurringBookingService.adminPause(id)));
    }

    @Operation(summary = "Admin resume recurring booking")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/{id}/resume")
    public ResponseEntity<ApiResponse<RecurringBookingResponse>> adminResume(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(recurringBookingService.adminResume(id)));
    }

    @Operation(summary = "Admin cancel recurring booking")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<ApiResponse<RecurringBookingResponse>> adminCancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(recurringBookingService.adminCancel(id)));
    }
}
