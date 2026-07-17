package com.project.booking.community.service;

import com.project.booking.community.dto.AdminModerationRequest;
import com.project.booking.community.dto.CommunityModerationHistoryResponse;
import com.project.booking.community.dto.CommunityPostResponse;
import com.project.booking.community.dto.CommunityReportResponse;
import com.project.booking.community.dto.CommunityViolationResponse;
import com.project.booking.community.dto.OwnerHideCommunityPostRequest;
import com.project.booking.community.dto.ReportCommunityPostRequest;
import com.project.booking.community.enums.CommunityReportStatus;
import com.project.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CommunityModerationService {
    void ensureCanPost(UUID userId);
    boolean isUserUnderModeration(UUID userId);
    CommunityReportResponse reportPost(UUID reporterId, UUID postId, ReportCommunityPostRequest request);
    CommunityPostResponse ownerHidePost(UUID ownerId, UUID postId, OwnerHideCommunityPostRequest request);
    CommunityModerationHistoryResponse review(UUID moderatorId, AdminModerationRequest request);
    CommunityPostResponse restorePost(UUID moderatorId, UUID postId, String reason, String note);
    PageResponse<CommunityReportResponse> getReports(CommunityReportStatus status, Pageable pageable);
    PageResponse<CommunityViolationResponse> getViolations(UUID userId, Pageable pageable);
    PageResponse<CommunityModerationHistoryResponse> getHistory(UUID userId, Pageable pageable);
    int expireViolations();
}
