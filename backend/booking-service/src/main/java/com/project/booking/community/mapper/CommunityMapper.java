package com.project.booking.community.mapper;

import com.project.booking.community.dto.CommunityApplicationResponse;
import com.project.booking.community.dto.CommunityModerationHistoryResponse;
import com.project.booking.community.dto.CommunityPostResponse;
import com.project.booking.community.dto.CommunityReportResponse;
import com.project.booking.community.dto.CommunityViolationResponse;
import com.project.booking.community.dto.MatchEvaluationResponse;
import com.project.booking.community.entity.CommunityApplication;
import com.project.booking.community.entity.CommunityModerationHistory;
import com.project.booking.community.entity.CommunityPost;
import com.project.booking.community.entity.CommunityPostReport;
import com.project.booking.community.entity.CommunityUserViolation;
import com.project.booking.community.entity.MatchEvaluation;
import org.springframework.stereotype.Component;

@Component
public class CommunityMapper {
    public CommunityPostResponse toPostResponse(CommunityPost post, boolean includeApplications) {
        return toPostResponse(post, includeApplications, false);
    }

    public CommunityPostResponse toPostResponse(CommunityPost post, boolean includeApplications, boolean ownerUnderModeration) {
        return CommunityPostResponse.builder()
                .id(post.getId())
                .bookingId(post.getBookingId())
                .ownerId(post.getOwnerId())
                .postType(post.getPostType())
                .status(post.getStatus())
                .title(post.getTitle())
                .description(post.getDescription())
                .skillLevel(post.getSkillLevel())
                .contactPhone(post.getContactPhone())
                .playersNeeded(post.getPlayersNeeded())
                .acceptedPlayersCount(post.getAcceptedPlayersCount())
                .bookingCode(post.getBookingCode())
                .fieldId(post.getFieldId())
                .fieldOwnerId(post.getFieldOwnerId())
                .fieldName(post.getFieldName())
                .subFieldId(post.getSubFieldId())
                .subFieldName(post.getSubFieldName())
                .fieldType(post.getFieldType())
                .bookingDate(post.getBookingDate())
                .startTime(post.getStartTime())
                .endTime(post.getEndTime())
                .ownerDisplayName(post.getOwnerDisplayName())
                .ownerAvatarUrl(post.getOwnerAvatarUrl())
                .ownerTeamPhotoUrl(post.getOwnerTeamPhotoUrl())
                .locationText(post.getLocationText())
                .matchedApplicationId(post.getMatchedApplicationId())
                .closedAt(post.getClosedAt())
                .hiddenAt(post.getHiddenAt())
                .hiddenReason(post.getHiddenReason())
                .ownerUnderModeration(ownerUnderModeration)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .applications(includeApplications
                        ? post.getApplications().stream().map(this::toApplicationResponse).toList()
                        : null)
                .build();
    }

    public CommunityApplicationResponse toApplicationResponse(CommunityApplication application) {
        return CommunityApplicationResponse.builder()
                .id(application.getId())
                .postId(application.getPost().getId())
                .applicantId(application.getApplicantId())
                .status(application.getStatus())
                .message(application.getMessage())
                .applicantDisplayName(application.getApplicantDisplayName())
                .applicantAvatarUrl(application.getApplicantAvatarUrl())
                .applicantTeamPhotoUrl(application.getApplicantTeamPhotoUrl())
                .applicantSkillLevel(application.getApplicantSkillLevel())
                .decidedAt(application.getDecidedAt())
                .withdrawnAt(application.getWithdrawnAt())
                .createdAt(application.getCreatedAt())
                .build();
    }

    public MatchEvaluationResponse toEvaluationResponse(MatchEvaluation evaluation) {
        return MatchEvaluationResponse.builder()
                .id(evaluation.getId())
                .postId(evaluation.getPostId())
                .bookingId(evaluation.getBookingId())
                .evaluatorId(evaluation.getEvaluatorId())
                .evaluatedUserId(evaluation.getEvaluatedUserId())
                .arrivedOnTime(evaluation.getArrivedOnTime())
                .cancelledUnexpectedly(evaluation.getCancelledUnexpectedly())
                .fairPlay(evaluation.getFairPlay())
                .wouldPlayAgain(evaluation.getWouldPlayAgain())
                .skillLevel(evaluation.getSkillLevel())
                .comment(evaluation.getComment())
                .createdAt(evaluation.getCreatedAt())
                .build();
    }

    public CommunityReportResponse toReportResponse(CommunityPostReport report, boolean includePost) {
        return CommunityReportResponse.builder()
                .id(report.getId())
                .postId(report.getPost().getId())
                .reporterId(report.getReporterId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .reviewedBy(report.getReviewedBy())
                .reviewedAt(report.getReviewedAt())
                .createdAt(report.getCreatedAt())
                .post(includePost ? toPostResponse(report.getPost(), false) : null)
                .build();
    }

    public CommunityViolationResponse toViolationResponse(CommunityUserViolation violation) {
        return CommunityViolationResponse.builder()
                .id(violation.getId())
                .userId(violation.getUserId())
                .reason(violation.getReason())
                .action(violation.getAction())
                .expireAt(violation.getExpireAt())
                .status(violation.getStatus())
                .sourcePostId(violation.getSourcePostId())
                .createdAt(violation.getCreatedAt())
                .build();
    }

    public CommunityModerationHistoryResponse toHistoryResponse(CommunityModerationHistory history) {
        return CommunityModerationHistoryResponse.builder()
                .id(history.getId())
                .targetUserId(history.getTargetUserId())
                .targetPostId(history.getTargetPostId())
                .moderatorId(history.getModeratorId())
                .action(history.getAction())
                .reason(history.getReason())
                .note(history.getNote())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
