package com.project.booking.community.service.impl;

import com.project.booking.community.dto.*;
import com.project.booking.community.entity.CommunityApplication;
import com.project.booking.community.entity.CommunityPost;
import com.project.booking.community.entity.MatchEvaluation;
import com.project.booking.community.enums.CommunityApplicationStatus;
import com.project.booking.community.enums.CommunityPostStatus;
import com.project.booking.community.enums.CommunityPostType;
import com.project.booking.community.kafka.CommunityNotificationEventPublisher;
import com.project.booking.community.kafka.MatchEvaluationEventPublisher;
import com.project.booking.community.mapper.CommunityMapper;
import com.project.booking.community.repository.CommunityApplicationRepository;
import com.project.booking.community.repository.CommunityPostRepository;
import com.project.booking.community.repository.MatchEvaluationRepository;
import com.project.booking.community.service.CommunityPostMaintenanceService;
import com.project.booking.community.service.CommunityPostService;
import com.project.booking.community.service.CommunityModerationService;
import com.project.booking.entity.Booking;
import com.project.booking.entity.UserProjection;
import com.project.booking.exception.BookingNotFoundException;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.UserProjectionRepository;
import com.project.common.dto.PageResponse;
import com.project.common.enums.BookingStatus;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.UnauthorizedException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CommunityPostServiceImpl implements CommunityPostService, CommunityPostMaintenanceService {
    private static final List<CommunityPostStatus> ACTIVE_POST_STATUSES = List.of(CommunityPostStatus.OPEN);
    private static final List<CommunityApplicationStatus> ACTIVE_APPLICATION_STATUSES = List.of(
            CommunityApplicationStatus.PENDING, CommunityApplicationStatus.ACCEPTED);

    private final CommunityPostRepository postRepository;
    private final CommunityApplicationRepository applicationRepository;
    private final MatchEvaluationRepository evaluationRepository;
    private final BookingRepository bookingRepository;
    private final CommunityMapper mapper;
    private final CommunityNotificationEventPublisher notifications;
    private final MatchEvaluationEventPublisher evaluationEvents;
    private final CommunityModerationService moderationService;
    private final UserProjectionRepository userProjectionRepository;

    @Override
    @Transactional
    public CommunityPostResponse create(UUID userId, CreateCommunityPostRequest request) {
        moderationService.ensureCanPost(userId);
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(request.getBookingId()));
        if (!booking.getClientId().equals(userId)) {
            throw new UnauthorizedException("Only the booking owner may create a community post");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Community posts require a confirmed booking");
        }
        if (!LocalDateTime.of(booking.getBookingDate(), booking.getStartTime()).isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Cannot create a post after the match has started");
        }
        if (postRepository.existsByBookingIdAndStatusIn(booking.getId(), ACTIVE_POST_STATUSES)) {
            throw new BadRequestException("This booking already has an active community post");
        }
        validatePlayersNeeded(request.getPostType(), request.getPlayersNeeded());

        CommunityPost post = CommunityPost.builder()
                .bookingId(booking.getId())
                .ownerId(userId)
                .postType(request.getPostType())
                .status(CommunityPostStatus.OPEN)
                .title(request.getTitle())
                .description(request.getDescription())
                .skillLevel(request.getSkillLevel())
                .contactPhone(request.getContactPhone())
                .playersNeeded(request.getPostType() == CommunityPostType.LOOKING_PLAYER ? request.getPlayersNeeded() : null)
                .bookingCode(booking.getBookingCode())
                .fieldId(booking.getSubField() != null ? booking.getSubField().getFieldId() : null)
                .fieldOwnerId(booking.getOwnerId())
                .fieldName(booking.getSubField() != null ? booking.getSubField().getFieldName() : null)
                .subFieldId(booking.getSubFieldId())
                .subFieldName(booking.getSubField() != null ? booking.getSubField().getName() : null)
                .fieldType(booking.getSubField() != null ? booking.getSubField().getSubFieldType() : null)
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .ownerDisplayName(request.getOwnerDisplayName())
                .ownerAvatarUrl(request.getOwnerAvatarUrl())
                .ownerTeamPhotoUrl(request.getOwnerTeamPhotoUrl())
                .build();
        return withOwnerStatistics(mapper.toPostResponse(postRepository.save(post), true));
    }

    @Override
    @Transactional
    public CommunityPostResponse update(UUID userId, UUID postId, UpdateCommunityPostRequest request) {
        moderationService.ensureCanPost(userId);
        CommunityPost post = ownedOpenPost(userId, postId);
        validatePlayersNeeded(post.getPostType(), request.getPlayersNeeded());
        post.setTitle(request.getTitle());
        post.setDescription(request.getDescription());
        post.setSkillLevel(request.getSkillLevel());
        post.setContactPhone(request.getContactPhone());
        post.setPlayersNeeded(post.getPostType() == CommunityPostType.LOOKING_PLAYER ? request.getPlayersNeeded() : null);
        return withOwnerStatistics(mapper.toPostResponse(post, true));
    }

    @Override
    @Transactional
    public CommunityPostResponse close(UUID userId, UUID postId) {
        CommunityPost post = ownedOpenPost(userId, postId);
        post.setStatus(CommunityPostStatus.CLOSED);
        post.setClosedAt(LocalDateTime.now());
        notifyApplicants(post, "COMMUNITY_POST_CLOSED", "Bai dang da dong");
        return withOwnerStatistics(mapper.toPostResponse(post, true));
    }

    @Override
    @Transactional
    public CommunityPostResponse markFull(UUID userId, UUID postId) {
        CommunityPost post = ownedOpenPost(userId, postId);
        if (post.getPostType() != CommunityPostType.LOOKING_PLAYER) {
            throw new BadRequestException("Only player recruitment posts can be marked full");
        }
        post.setStatus(CommunityPostStatus.FULL);
        post.setClosedAt(LocalDateTime.now());
        notifyApplicants(post, "COMMUNITY_PLAYER_RECRUITMENT_FULL", "Da tuyen du nguoi cho tran dau");
        return withOwnerStatistics(mapper.toPostResponse(post, true));
    }

    @Override
    @Transactional(readOnly = true)
    public CommunityPostResponse get(UUID viewerId, UUID postId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new BadRequestException("Community post not found"));
        if (post.getStatus() == CommunityPostStatus.HIDDEN) {
            throw new BadRequestException("Post unavailable");
        }
        return withOwnerStatistics(mapper.toPostResponse(post, post.getOwnerId().equals(viewerId),
                moderationService.isUserUnderModeration(post.getOwnerId())));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommunityPostResponse> search(CommunityPostSearchRequest request, Pageable pageable) {
        return PageResponse.from(postRepository.findAll(spec(request), pageable)
                .map(post -> withOwnerStatistics(mapper.toPostResponse(post, false))));
    }

    @Override
    @Transactional
    public CommunityApplicationResponse apply(UUID userId, UUID postId, CommunityApplicationRequest request) {
        moderationService.ensureCanPost(userId);
        CommunityPost post = openPost(postId);
        if (post.getOwnerId().equals(userId)) {
            throw new BadRequestException("You cannot apply to your own post");
        }
        if (applicationRepository.existsActiveApplication(postId, userId, ACTIVE_APPLICATION_STATUSES)) {
            throw new BadRequestException("You already have an active application for this post");
        }
        CommunityApplication application = CommunityApplication.builder()
                .post(post)
                .applicantId(userId)
                .status(CommunityApplicationStatus.PENDING)
                .message(request.getMessage())
                .applicantDisplayName(request.getApplicantDisplayName())
                .applicantAvatarUrl(request.getApplicantAvatarUrl())
                .applicantTeamPhotoUrl(request.getApplicantTeamPhotoUrl())
                .applicantSkillLevel(request.getApplicantSkillLevel())
                .build();
        CommunityApplication saved = applicationRepository.save(application);
        notifications.publish(post.getOwnerId(), "COMMUNITY_POST_APPLIED", "Co nguoi vua ung tuyen",
                payload(post, Map.of("applicationId", saved.getId(), "applicantId", userId)));
        return mapper.toApplicationResponse(saved);
    }

    @Override
    @Transactional
    public CommunityApplicationResponse withdraw(UUID userId, UUID postId) {
        CommunityApplication application = applicationRepository
                .findByPostIdAndApplicantIdAndStatus(postId, userId, CommunityApplicationStatus.PENDING)
                .orElseThrow(() -> new BadRequestException("No pending application found"));
        application.setStatus(CommunityApplicationStatus.WITHDRAWN);
        application.setWithdrawnAt(LocalDateTime.now());
        notifications.publish(application.getPost().getOwnerId(), "COMMUNITY_APPLICATION_WITHDRAWN", "Ung vien da rut yeu cau",
                payload(application.getPost(), Map.of("applicationId", application.getId(), "applicantId", userId)));
        return mapper.toApplicationResponse(application);
    }

    @Override
    @Transactional
    public CommunityApplicationResponse accept(UUID userId, UUID postId, UUID applicationId) {
        CommunityPost post = ownedOpenPost(userId, postId);
        CommunityApplication application = pendingApplication(postId, applicationId);
        application.setStatus(CommunityApplicationStatus.ACCEPTED);
        application.setDecidedAt(LocalDateTime.now());
        post.setAcceptedPlayersCount(post.getAcceptedPlayersCount() + 1);
        if (post.getPostType() == CommunityPostType.LOOKING_OPPONENT) {
            post.setStatus(CommunityPostStatus.MATCHED);
            post.setMatchedApplicationId(application.getId());
            post.setClosedAt(LocalDateTime.now());
            applicationRepository.rejectOtherPendingApplications(postId, applicationId,
                    CommunityApplicationStatus.PENDING, CommunityApplicationStatus.REJECTED);
            notifications.publish(application.getApplicantId(), "COMMUNITY_OPPONENT_MATCHED", "Doi cua ban da duoc chap nhan",
                    payload(post, Map.of("applicationId", applicationId)));
        } else {
            notifications.publish(application.getApplicantId(), "COMMUNITY_APPLICATION_ACCEPTED", "Yeu cau tham gia da duoc chap nhan",
                    payload(post, Map.of("applicationId", applicationId)));
        }
        return mapper.toApplicationResponse(application);
    }

    @Override
    @Transactional
    public CommunityApplicationResponse reject(UUID userId, UUID postId, UUID applicationId) {
        ownedOpenPost(userId, postId);
        CommunityApplication application = pendingApplication(postId, applicationId);
        application.setStatus(CommunityApplicationStatus.REJECTED);
        application.setDecidedAt(LocalDateTime.now());
        notifications.publish(application.getApplicantId(), "COMMUNITY_APPLICATION_REJECTED", "Yeu cau tham gia da bi tu choi",
                payload(application.getPost(), Map.of("applicationId", applicationId)));
        return mapper.toApplicationResponse(application);
    }

    @Override
    @Transactional
    public MatchEvaluationResponse evaluate(UUID userId, UUID postId, MatchEvaluationRequest request) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new BadRequestException("Community post not found"));
        if (post.getPostType() != CommunityPostType.LOOKING_OPPONENT || post.getStatus() != CommunityPostStatus.MATCHED) {
            throw new BadRequestException("Evaluation only applies to matched opponent posts");
        }
        if (!post.getOwnerId().equals(userId) && post.getApplications().stream()
                .noneMatch(app -> app.getApplicantId().equals(userId) && app.getStatus() == CommunityApplicationStatus.ACCEPTED)) {
            throw new UnauthorizedException("Only matched participants may evaluate this match");
        }
        if (evaluationRepository.existsByPostIdAndEvaluatorIdAndEvaluatedUserId(postId, userId, request.getEvaluatedUserId())) {
            throw new BadRequestException("Evaluation already submitted");
        }
        MatchEvaluation evaluation = MatchEvaluation.builder()
                .postId(postId)
                .bookingId(post.getBookingId())
                .evaluatorId(userId)
                .evaluatedUserId(request.getEvaluatedUserId())
                .arrivedOnTime(request.getArrivedOnTime())
                .cancelledUnexpectedly(request.getCancelledUnexpectedly())
                .fairPlay(request.getFairPlay())
                .wouldPlayAgain(request.getWouldPlayAgain())
                .comment(request.getComment())
                .build();
        MatchEvaluation saved = evaluationRepository.save(evaluation);
        evaluationEvents.publish(saved);
        return mapper.toEvaluationResponse(saved);
    }

    @Override
    @Transactional
    public void cancelOpenPostForBooking(UUID bookingId) {
        postRepository.cancelOpenPostForBooking(bookingId, CommunityPostStatus.OPEN, CommunityPostStatus.CANCELLED);
    }

    @Override
    @Transactional
    public int closeStartedOpenPosts() {
        LocalDateTime now = LocalDateTime.now();
        List<CommunityPost> posts = postRepository.findStartedOpenPosts(
                CommunityPostStatus.OPEN, now.toLocalDate(), now.toLocalTime());
        posts.forEach(post -> {
            post.setStatus(CommunityPostStatus.CLOSED);
            post.setClosedAt(now);
            notifyApplicants(post, "COMMUNITY_POST_CLOSED", "Bai dang da dong");
        });
        return posts.size();
    }

    private void validatePlayersNeeded(CommunityPostType postType, Integer playersNeeded) {
        if (postType == CommunityPostType.LOOKING_PLAYER && playersNeeded == null) {
            throw new BadRequestException("Players needed is required for player recruitment posts");
        }
        if (postType == CommunityPostType.LOOKING_OPPONENT && playersNeeded != null) {
            throw new BadRequestException("Players needed is only available for player recruitment posts");
        }
    }

    private CommunityPost openPost(UUID postId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new BadRequestException("Community post not found"));
        if (post.getStatus() != CommunityPostStatus.OPEN) {
            throw new BadRequestException("This community post is not open");
        }
        return post;
    }

    private CommunityPost ownedOpenPost(UUID userId, UUID postId) {
        CommunityPost post = openPost(postId);
        if (!post.getOwnerId().equals(userId)) {
            throw new UnauthorizedException("Only the post owner can perform this action");
        }
        return post;
    }

    private CommunityApplication pendingApplication(UUID postId, UUID applicationId) {
        CommunityApplication application = applicationRepository.findByIdAndPostId(applicationId, postId)
                .orElseThrow(() -> new BadRequestException("Application not found"));
        if (application.getStatus() != CommunityApplicationStatus.PENDING) {
            throw new BadRequestException("Only pending applications can be changed");
        }
        return application;
    }

    private Specification<CommunityPost> spec(CommunityPostSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.getStatus() == CommunityPostStatus.HIDDEN) {
                predicates.add(cb.disjunction());
            } else {
                predicates.add(cb.equal(root.get("status"), request.getStatus() != null ? request.getStatus() : CommunityPostStatus.OPEN));
            }
            if (request.getPostType() != null) predicates.add(cb.equal(root.get("postType"), request.getPostType()));
            if (request.getSkillLevel() != null && !request.getSkillLevel().isBlank()) predicates.add(cb.equal(root.get("skillLevel"), request.getSkillLevel()));
            if (request.getDate() != null) predicates.add(cb.equal(root.get("bookingDate"), request.getDate()));
            if (request.getFieldType() != null) predicates.add(cb.equal(root.get("fieldType"), request.getFieldType()));
            if (request.getDistrict() != null && !request.getDistrict().isBlank()) predicates.add(cb.like(cb.lower(root.get("locationText")), "%" + request.getDistrict().toLowerCase(Locale.ROOT) + "%"));
            if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
                String keyword = "%" + request.getKeyword().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword),
                        cb.like(cb.lower(root.get("fieldName")), keyword),
                        cb.like(cb.lower(root.get("subFieldName")), keyword)));
            }
            if ("upcoming".equalsIgnoreCase(request.getSortBy())) {
                query.orderBy(cb.asc(root.get("bookingDate")), cb.asc(root.get("startTime")));
            } else {
                query.orderBy(cb.desc(root.get("createdAt")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void notifyApplicants(CommunityPost post, String code, String title) {
        post.getApplications().stream()
                .filter(app -> app.getStatus() == CommunityApplicationStatus.PENDING || app.getStatus() == CommunityApplicationStatus.ACCEPTED)
                .forEach(app -> notifications.publish(app.getApplicantId(), code, title,
                        payload(post, Map.of("applicationId", app.getId()))));
    }

    private Map<String, Object> payload(CommunityPost post, Map<String, Object> extra) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("postId", post.getId());
        payload.put("bookingId", post.getBookingId());
        payload.put("postType", post.getPostType().name());
        payload.put("fieldName", post.getFieldName());
        payload.put("bookingDate", post.getBookingDate());
        payload.put("startTime", post.getStartTime());
        payload.putAll(extra);
        return payload;
    }

    private CommunityPostResponse withOwnerStatistics(CommunityPostResponse response) {
        if (response.getPostType() != CommunityPostType.LOOKING_OPPONENT) {
            return response;
        }
        userProjectionRepository.findById(response.getOwnerId())
                .map(this::toStatistics)
                .ifPresent(response::setOwnerStatistics);
        return response;
    }

    private CommunityPlayerStatisticsResponse toStatistics(UserProjection projection) {
        int totalMatches = projection.getTotalMatches() == null ? 0 : projection.getTotalMatches();
        int wins = projection.getWins() == null ? 0 : projection.getWins();
        BigDecimal winRate = totalMatches == 0
                ? BigDecimal.ZERO.setScale(1)
                : BigDecimal.valueOf(wins)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalMatches), 1, RoundingMode.HALF_UP);
        return CommunityPlayerStatisticsResponse.builder()
                .totalMatches(totalMatches)
                .winRate(winRate)
                .onTimeRate(projection.getOnTimeRate())
                .noCancelRate(projection.getNoCancelRate())
                .fairPlayRate(projection.getFairPlayRate())
                .completedBookingCount(projection.getCompletedBookingCount())
                .build();
    }
}
