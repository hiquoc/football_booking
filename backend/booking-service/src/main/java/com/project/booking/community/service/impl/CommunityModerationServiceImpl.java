package com.project.booking.community.service.impl;

import com.project.booking.community.dto.*;
import com.project.booking.community.entity.*;
import com.project.booking.community.enums.*;
import com.project.booking.community.kafka.CommunityNotificationEventPublisher;
import com.project.booking.community.mapper.CommunityMapper;
import com.project.booking.community.repository.*;
import com.project.booking.community.service.CommunityModerationService;
import com.project.booking.moderation.service.BookingModerationService;
import com.project.common.dto.PageResponse;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.NotFoundException;
import com.project.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityModerationServiceImpl implements CommunityModerationService {
    private static final List<CommunityViolationStatus> BLOCKING_STATUSES = List.of(
            CommunityViolationStatus.ACTIVE, CommunityViolationStatus.PERMANENT);
    private static final List<CommunityPostStatus> HIDE_ON_BAN_STATUSES = List.of(
            CommunityPostStatus.OPEN, CommunityPostStatus.FULL);

    private final CommunityPostRepository postRepository;
    private final CommunityApplicationRepository applicationRepository;
    private final CommunityPostReportRepository reportRepository;
    private final CommunityUserViolationRepository violationRepository;
    private final CommunityModerationHistoryRepository historyRepository;
    private final CommunityMapper mapper;
    private final CommunityNotificationEventPublisher notifications;
    private final BookingModerationService bookingModerationService;

    @Value("${community.moderation.permanent-ban-threshold:3}")
    private int permanentBanThreshold;

    @Override
    @Transactional(readOnly = true)
    public void ensureCanPost(UUID userId) {
        bookingModerationService.ensurePlatformAllowed(userId);
        boolean temporaryBanned = violationRepository.existsByUserIdAndActionAndStatus(
                userId, CommunityModerationAction.TEMPORARY_POSTING_BAN, CommunityViolationStatus.ACTIVE);
        boolean permanentlyBanned = violationRepository.existsByUserIdAndStatus(userId, CommunityViolationStatus.PERMANENT);
        if (temporaryBanned || permanentlyBanned) {
            throw new UnauthorizedException("You are currently restricted from community posting", "COMMUNITY_POSTING_RESTRICTED");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserUnderModeration(UUID userId) {
        return violationRepository.countByUserIdAndStatusIn(userId, BLOCKING_STATUSES) > 0;
    }

    @Override
    @Transactional
    public CommunityReportResponse reportPost(UUID reporterId, UUID postId, ReportCommunityPostRequest request) {
        bookingModerationService.ensurePlatformAllowed(reporterId);
        CommunityPost post = findPost(postId);
        if (post.getOwnerId().equals(reporterId)) {
            throw new BadRequestException("You cannot report your own community post");
        }
        if (List.of(CommunityPostStatus.CLOSED, CommunityPostStatus.CANCELLED, CommunityPostStatus.HIDDEN).contains(post.getStatus())) {
            throw new BadRequestException("This community post can no longer be reported", "OPERATION_NOT_ALLOWED");
        }
        if (reportRepository.existsByPostIdAndReporterId(postId, reporterId)) {
            throw new BadRequestException("You already reported this community post", "POST_ALREADY_REPORTED");
        }
        CommunityPostReport report = CommunityPostReport.builder()
                .post(post)
                .reporterId(reporterId)
                .reason(request.getReason())
                .description(request.getDescription())
                .status(CommunityReportStatus.PENDING)
                .build();
        return mapper.toReportResponse(reportRepository.save(report), false);
    }

    @Override
    @Transactional
    public CommunityPostResponse ownerHidePost(UUID ownerId, UUID postId, OwnerHideCommunityPostRequest request) {
        CommunityPost post = findPost(postId);
        if (!ownerId.equals(post.getFieldOwnerId())) {
            throw new UnauthorizedException("Only the field owner can hide posts for this booking");
        }
        hidePost(post, request.getReason());
        rejectPendingApplications(post);
        saveHistory(post.getOwnerId(), post.getId(), ownerId, CommunityModerationAction.OWNER_HIDE_POST,
                request.getReason(), null);
        notifications.publish(post.getOwnerId(), "COMMUNITY_POST_HIDDEN", "Bai dang cong dong da bi an",
                payload(post, Map.of("reason", request.getReason())));
        return mapper.toPostResponse(post, false, isUserUnderModeration(post.getOwnerId()));
    }

    @Override
    @Transactional
    public CommunityModerationHistoryResponse review(UUID moderatorId, AdminModerationRequest request) {
        CommunityPost post = request.getTargetPostId() != null ? findPost(request.getTargetPostId()) : null;
        UUID targetUserId = request.getTargetUserId() != null
                ? request.getTargetUserId()
                : post != null ? post.getOwnerId() : null;
        UUID postId = post != null ? post.getId() : null;
        if (targetUserId == null) {
            throw new BadRequestException("A target user or target post is required");
        }

        switch (request.getAction()) {
            case NO_ACTION -> markReportsReviewed(post, moderatorId);
            case HIDE_POST -> {
                requirePost(post);
                hidePost(post, request.getReason());
                rejectPendingApplications(post);
                notifications.publish(post.getOwnerId(), "COMMUNITY_POST_HIDDEN", "Bai dang cong dong da bi an",
                        payload(post, Map.of("reason", request.getReason())));
                markReportsReviewed(post, moderatorId);
            }
            case ISSUE_WARNING -> {
                requirePost(post);
                createViolation(targetUserId, postId, request, CommunityViolationStatus.ACTIVE, "COMMUNITY_MODERATION_WARNING");
                hidePost(post, request.getReason());
                markReportsReviewed(post, moderatorId);
            }
            case TEMPORARY_POSTING_BAN -> {
                if (request.getExpireAt() == null) {
                    throw new BadRequestException("Temporary posting ban requires an expiration time");
                }
                createViolation(targetUserId, postId, request, CommunityViolationStatus.ACTIVE, "COMMUNITY_TEMPORARY_POSTING_BAN");
                hidePostsAfterBan(targetUserId, moderatorId, request.getReason());
                enforceThreshold(targetUserId, moderatorId, request.getReason());
                markReportsReviewed(post, moderatorId);
            }
            case PERMANENT_POSTING_BAN -> {
                createViolation(targetUserId, postId, request, CommunityViolationStatus.PERMANENT, "COMMUNITY_PERMANENT_POSTING_BAN");
                hidePostsAfterBan(targetUserId, moderatorId, request.getReason());
                markReportsReviewed(post, moderatorId);
            }
            case RESTORE_POST -> {
                requirePost(post);
                restorePostState(post, request.getReason());
            }
            default -> throw new BadRequestException("Unsupported moderation action for admin review");
        }

        CommunityModerationHistory history = saveHistory(targetUserId, post != null ? post.getId() : null, moderatorId,
                request.getAction(), request.getReason(), request.getNote());
        return mapper.toHistoryResponse(history);
    }

    @Override
    @Transactional
    public CommunityPostResponse restorePost(UUID moderatorId, UUID postId, String reason, String note) {
        CommunityPost post = findPost(postId);
        restorePostState(post, reason);
        saveHistory(post.getOwnerId(), postId, moderatorId, CommunityModerationAction.RESTORE_POST, reason, note);
        return mapper.toPostResponse(post, false, isUserUnderModeration(post.getOwnerId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommunityReportResponse> getReports(CommunityReportStatus status, Pageable pageable) {
        return PageResponse.from(reportRepository.findForReview(status, pageable)
                .map(report -> mapper.toReportResponse(report, true)));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommunityViolationResponse> getViolations(UUID userId, Pageable pageable) {
        return PageResponse.from(violationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(mapper::toViolationResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommunityModerationHistoryResponse> getHistory(UUID userId, Pageable pageable) {
        return PageResponse.from(historyRepository.findByTargetUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(mapper::toHistoryResponse));
    }

    @Override
    @Transactional
    public int expireViolations() {
        return violationRepository.expireTemporaryViolations(
                CommunityViolationStatus.ACTIVE, CommunityViolationStatus.EXPIRED, LocalDateTime.now());
    }

    private CommunityPost findPost(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Community post not found", "POST_NOT_FOUND"));
    }

    private void requirePost(CommunityPost post) {
        if (post == null) {
            throw new BadRequestException("This moderation action requires a target post");
        }
    }

    private void hidePost(CommunityPost post, String reason) {
        post.setStatus(CommunityPostStatus.HIDDEN);
        post.setHiddenAt(LocalDateTime.now());
        post.setHiddenReason(reason);
    }

    private void restorePostState(CommunityPost post, String reason) {
        if (post.getStatus() != CommunityPostStatus.HIDDEN) {
            throw new BadRequestException("Only hidden posts can be restored");
        }
        post.setStatus(CommunityPostStatus.OPEN);
        post.setHiddenAt(null);
        post.setHiddenReason(reason);
    }

    private void rejectPendingApplications(CommunityPost post) {
        List<CommunityApplication> pendingApplications = applicationRepository.findByPostIdAndStatus(
                post.getId(), CommunityApplicationStatus.PENDING);
        int rejected = applicationRepository.rejectPendingApplicationsForPost(
                post.getId(), CommunityApplicationStatus.PENDING, CommunityApplicationStatus.REJECTED);
        pendingApplications.forEach(application -> notifications.publish(
                application.getApplicantId(),
                "COMMUNITY_APPLICATION_AUTO_REJECTED",
                "Ung tuyen dang cho da bi tu choi do bai dang bi an",
                payload(post, Map.of("applicationId", application.getId(), "rejectedCount", rejected))));
    }

    private void createViolation(UUID targetUserId, UUID postId, AdminModerationRequest request,
                                 CommunityViolationStatus status, String notificationCode) {
        if (postId != null && violationRepository.existsByUserIdAndSourcePostIdAndAction(
                targetUserId, postId, request.getAction())) {
            throw new BadRequestException("Violation for this user and post already exists", "DUPLICATE_REQUEST");
        }
        CommunityUserViolation violation = CommunityUserViolation.builder()
                .userId(targetUserId)
                .reason(request.getReason())
                .action(request.getAction())
                .expireAt(status == CommunityViolationStatus.ACTIVE ? request.getExpireAt() : null)
                .status(status)
                .sourcePostId(request.getTargetPostId())
                .build();
        violationRepository.save(violation);
        Map<String, Object> notificationPayload = new LinkedHashMap<>();
        notificationPayload.put("reason", request.getReason());
        if (request.getExpireAt() != null) {
            notificationPayload.put("expireAt", request.getExpireAt());
        }
        notifications.publish(targetUserId, notificationCode, notificationTitle(notificationCode),
                notificationPayload);
    }

    private void enforceThreshold(UUID targetUserId, UUID moderatorId, String reason) {
        long activeCount = violationRepository.countByUserIdAndStatus(targetUserId, CommunityViolationStatus.ACTIVE);
        if (activeCount < permanentBanThreshold
                || violationRepository.existsByUserIdAndStatus(targetUserId, CommunityViolationStatus.PERMANENT)) {
            return;
        }
        AdminModerationRequest request = new AdminModerationRequest();
        request.setAction(CommunityModerationAction.PERMANENT_POSTING_BAN);
        request.setTargetUserId(targetUserId);
        request.setReason("Automatic permanent ban after " + activeCount + " active violations: " + reason);
        createViolation(targetUserId, null, request, CommunityViolationStatus.PERMANENT, "COMMUNITY_PERMANENT_POSTING_BAN");
        saveHistory(targetUserId, null, moderatorId, CommunityModerationAction.PERMANENT_POSTING_BAN,
                request.getReason(), "Automatic threshold enforcement");
    }

    private void hidePostsAfterBan(UUID targetUserId, UUID moderatorId, String reason) {
        postRepository.findByOwnerIdAndStatusIn(targetUserId, HIDE_ON_BAN_STATUSES).forEach(post -> {
            hidePost(post, reason);
            rejectPendingApplications(post);
            saveHistory(targetUserId, post.getId(), moderatorId,
                    CommunityModerationAction.AUTO_HIDE_POSTS_AFTER_BAN, reason, null);
        });
    }

    private void markReportsReviewed(CommunityPost post, UUID moderatorId) {
        if (post == null) {
            return;
        }
        reportRepository.findAll().stream()
                .filter(report -> report.getPost().getId().equals(post.getId())
                        && report.getStatus() == CommunityReportStatus.PENDING)
                .forEach(report -> {
                    report.setStatus(CommunityReportStatus.REVIEWED);
                    report.setReviewedBy(moderatorId);
                    report.setReviewedAt(LocalDateTime.now());
                });
    }

    private CommunityModerationHistory saveHistory(UUID targetUserId, UUID targetPostId, UUID moderatorId,
                                                   CommunityModerationAction action, String reason, String note) {
        return historyRepository.save(CommunityModerationHistory.builder()
                .targetUserId(targetUserId)
                .targetPostId(targetPostId)
                .moderatorId(moderatorId)
                .action(action)
                .reason(reason)
                .note(note)
                .build());
    }

    private String notificationTitle(String code) {
        return switch (code) {
            case "COMMUNITY_MODERATION_WARNING" -> "Canh bao vi pham cong dong";
            case "COMMUNITY_TEMPORARY_POSTING_BAN" -> "Ban tam thoi bi han che dang bai cong dong";
            case "COMMUNITY_PERMANENT_POSTING_BAN" -> "Ban da bi cam dang bai cong dong";
            default -> "Thong bao kiem duyet cong dong";
        };
    }

    private Map<String, Object> payload(CommunityPost post, Map<String, Object> extra) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("postId", post.getId());
        payload.put("ownerId", post.getOwnerId());
        payload.put("fieldName", post.getFieldName());
        payload.putAll(extra);
        return payload;
    }
}
