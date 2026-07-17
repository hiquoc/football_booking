package com.project.booking.community.controller;

import com.project.booking.community.dto.AdminModerationRequest;
import com.project.booking.community.dto.CommunityModerationHistoryResponse;
import com.project.booking.community.dto.CommunityPostResponse;
import com.project.booking.community.dto.CommunityReportResponse;
import com.project.booking.community.dto.CommunityViolationResponse;
import com.project.booking.community.enums.CommunityReportStatus;
import com.project.booking.community.service.CommunityModerationService;
import com.project.common.dto.ApiResponse;
import com.project.common.dto.PageResponse;
import com.project.common.security.CurrentUser;
import com.project.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/community-moderation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Community Moderation", description = "Admin moderation for community reports, violations, bans, and audit history")
public class CommunityModerationController {
    private final CommunityModerationService service;

    @Operation(summary = "List reported community posts")
    @GetMapping("/reports")
    @PageableAsQueryParam
    public ApiResponse<PageResponse<CommunityReportResponse>> reports(
            @RequestParam(required = false) CommunityReportStatus status,
            Pageable pageable) {
        return ApiResponse.success(service.getReports(status, pageable));
    }

    @Operation(summary = "Apply a moderation review action")
    @PostMapping("/reviews")
    public ApiResponse<CommunityModerationHistoryResponse> review(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody AdminModerationRequest request) {
        return ApiResponse.success(service.review(user.id(), request));
    }

    @Operation(summary = "Restore a hidden community post")
    @PatchMapping("/posts/{postId}/restore")
    public ApiResponse<CommunityPostResponse> restore(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID postId,
            @Valid @RequestBody RestorePostRequest request) {
        return ApiResponse.success(service.restorePost(user.id(), postId, request.getReason(), request.getNote()));
    }

    @Operation(summary = "View community violation history for a user")
    @GetMapping("/users/{userId}/violations")
    @PageableAsQueryParam
    public ApiResponse<PageResponse<CommunityViolationResponse>> violations(
            @PathVariable UUID userId,
            Pageable pageable) {
        return ApiResponse.success(service.getViolations(userId, pageable));
    }

    @Operation(summary = "View community moderation audit history for a user")
    @GetMapping("/users/{userId}/history")
    @PageableAsQueryParam
    public ApiResponse<PageResponse<CommunityModerationHistoryResponse>> history(
            @PathVariable UUID userId,
            Pageable pageable) {
        return ApiResponse.success(service.getHistory(userId, pageable));
    }

    @Data
    public static class RestorePostRequest {
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(max = 500)
        private String reason;

        @jakarta.validation.constraints.Size(max = 1000)
        private String note;
    }
}
