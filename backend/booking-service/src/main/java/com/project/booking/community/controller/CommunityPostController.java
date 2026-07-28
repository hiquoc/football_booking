package com.project.booking.community.controller;

import com.project.booking.community.dto.*;
import com.project.booking.community.service.CommunityModerationService;
import com.project.booking.community.service.CommunityPostService;
import com.project.common.dto.ApiResponse;
import com.project.common.dto.PageResponse;
import com.project.common.security.CurrentUser;
import com.project.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/community-posts")
@RequiredArgsConstructor
@Tag(name = "Community Match Posts", description = "Opponent and player recruitment posts tied to confirmed bookings")
public class CommunityPostController {
    private final CommunityPostService service;
    private final CommunityModerationService moderationService;

    @Operation(summary = "Search community feed")
    @GetMapping
    @PageableAsQueryParam
    public ApiResponse<PageResponse<CommunityPostResponse>> search(CommunityPostSearchRequest request, Pageable pageable) {
        return ApiResponse.success(service.search(request, pageable));
    }

    @Operation(summary = "Create a community post")
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE')")
    @PostMapping
    public ResponseEntity<ApiResponse<CommunityPostResponse>> create(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody CreateCommunityPostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Community post created", service.create(user.id(), request)));
    }

    @Operation(summary = "Get community post detail")
    @GetMapping("/{postId}")
    public ApiResponse<CommunityPostResponse> get(@CurrentUser UserPrincipal user, @PathVariable UUID postId) {
        return ApiResponse.success(service.get(user != null ? user.id() : null, postId));
    }

    @Operation(summary = "Report an inappropriate community post")
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE','OWNER','ADMIN')")
    @PostMapping("/{postId}/reports")
    public ResponseEntity<ApiResponse<CommunityReportResponse>> report(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID postId,
            @Valid @RequestBody ReportCommunityPostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Community post reported", moderationService.reportPost(user.id(), postId, request)));
    }

    @Operation(summary = "Field owner hides an inappropriate post for their field booking")
    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/{postId}/owner-hide")
    public ApiResponse<CommunityPostResponse> ownerHide(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID postId,
            @Valid @RequestBody OwnerHideCommunityPostRequest request) {
        return ApiResponse.success(moderationService.ownerHidePost(user.id(), postId, request));
    }

    @Operation(summary = "Edit own open post")
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE')")
    @PutMapping("/{postId}")
    public ApiResponse<CommunityPostResponse> update(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID postId,
            @Valid @RequestBody UpdateCommunityPostRequest request) {
        return ApiResponse.success(service.update(user.id(), postId, request));
    }

    @Operation(summary = "Close own open post")
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE')")
    @PatchMapping("/{postId}/close")
    public ApiResponse<CommunityPostResponse> close(@CurrentUser UserPrincipal user, @PathVariable UUID postId) {
        return ApiResponse.success(service.close(user.id(), postId));
    }

    @Operation(summary = "Mark player recruitment as full")
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE')")
    @PatchMapping("/{postId}/full")
    public ApiResponse<CommunityPostResponse> markFull(@CurrentUser UserPrincipal user, @PathVariable UUID postId) {
        return ApiResponse.success(service.markFull(user.id(), postId));
    }

    @Operation(summary = "Apply to a community post")
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE')")
    @PostMapping("/{postId}/applications")
    public ResponseEntity<ApiResponse<CommunityApplicationResponse>> apply(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID postId,
            @Valid @RequestBody CommunityApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application submitted", service.apply(user.id(), postId, request)));
    }

    @Operation(summary = "Withdraw own pending application")
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE')")
    @PatchMapping("/{postId}/applications/withdraw")
    public ApiResponse<CommunityApplicationResponse> withdraw(@CurrentUser UserPrincipal user, @PathVariable UUID postId) {
        return ApiResponse.success(service.withdraw(user.id(), postId));
    }

    @Operation(summary = "Accept an application")
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE')")
    @PatchMapping("/{postId}/applications/{applicationId}/accept")
    public ApiResponse<CommunityApplicationResponse> accept(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID postId,
            @PathVariable UUID applicationId) {
        return ApiResponse.success(service.accept(user.id(), postId, applicationId));
    }

    @Operation(summary = "Reject an application")
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE')")
    @PatchMapping("/{postId}/applications/{applicationId}/reject")
    public ApiResponse<CommunityApplicationResponse> reject(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID postId,
            @PathVariable UUID applicationId) {
        return ApiResponse.success(service.reject(user.id(), postId, applicationId));
    }

    @Operation(summary = "Submit opponent match evaluation")
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE')")
    @PostMapping("/{postId}/evaluations")
    public ResponseEntity<ApiResponse<MatchEvaluationResponse>> evaluate(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID postId,
            @Valid @RequestBody MatchEvaluationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Evaluation submitted", service.evaluate(user.id(), postId, request)));
    }
}
