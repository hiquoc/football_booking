package com.project.booking.community.service.impl;

import com.project.booking.community.dto.AdminModerationRequest;
import com.project.booking.community.entity.CommunityModerationHistory;
import com.project.booking.community.entity.CommunityPost;
import com.project.booking.community.entity.CommunityUserViolation;
import com.project.booking.community.enums.CommunityModerationAction;
import com.project.booking.community.enums.CommunityPostStatus;
import com.project.booking.community.enums.CommunityPostType;
import com.project.booking.community.enums.CommunityViolationStatus;
import com.project.booking.community.kafka.CommunityNotificationEventPublisher;
import com.project.booking.community.mapper.CommunityMapper;
import com.project.booking.community.repository.CommunityApplicationRepository;
import com.project.booking.community.repository.CommunityModerationHistoryRepository;
import com.project.booking.community.repository.CommunityPostReportRepository;
import com.project.booking.community.repository.CommunityPostRepository;
import com.project.booking.community.repository.CommunityUserViolationRepository;
import com.project.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityModerationServiceImplTest {

    @Mock
    private CommunityPostRepository postRepository;
    @Mock
    private CommunityApplicationRepository applicationRepository;
    @Mock
    private CommunityPostReportRepository reportRepository;
    @Mock
    private CommunityUserViolationRepository violationRepository;
    @Mock
    private CommunityModerationHistoryRepository historyRepository;
    @Spy
    private CommunityMapper mapper;
    @Mock
    private CommunityNotificationEventPublisher notifications;

    @InjectMocks
    private CommunityModerationServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "permanentBanThreshold", 3);
    }

    @Test
    void reviewRejectsDuplicateViolationForSameUserPostAndAction() {
        UUID moderatorId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        CommunityPost post = post(ownerId, postId);
        AdminModerationRequest request = request(CommunityModerationAction.ISSUE_WARNING, postId, null);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(violationRepository.existsByUserIdAndSourcePostIdAndAction(
                ownerId, postId, CommunityModerationAction.ISSUE_WARNING))
                .thenReturn(true);

        assertThatThrownBy(() -> service.review(moderatorId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Violation for this user and post already exists");

        assertThat(post.getStatus()).isEqualTo(CommunityPostStatus.OPEN);
        verify(violationRepository, never()).save(any(CommunityUserViolation.class));
        verify(historyRepository, never()).save(any(CommunityModerationHistory.class));
        verify(notifications, never()).publish(any(), any(), any(), any());
    }

    @Test
    void reviewAllowsDifferentEnforcementActionForSameUserAndPost() {
        UUID moderatorId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        CommunityPost post = post(ownerId, postId);
        AdminModerationRequest request = request(CommunityModerationAction.TEMPORARY_POSTING_BAN, postId, null);
        LocalDateTime expireAt = LocalDateTime.now().plusDays(7);
        request.setExpireAt(expireAt);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(violationRepository.existsByUserIdAndSourcePostIdAndAction(
                ownerId, postId, CommunityModerationAction.TEMPORARY_POSTING_BAN))
                .thenReturn(false);
        when(postRepository.findByOwnerIdAndStatusIn(eq(ownerId), any())).thenReturn(List.of());
        when(historyRepository.save(any(CommunityModerationHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.review(moderatorId, request);

        ArgumentCaptor<CommunityUserViolation> violationCaptor = ArgumentCaptor.forClass(CommunityUserViolation.class);
        verify(violationRepository).save(violationCaptor.capture());
        CommunityUserViolation violation = violationCaptor.getValue();
        assertThat(violation.getUserId()).isEqualTo(ownerId);
        assertThat(violation.getSourcePostId()).isEqualTo(postId);
        assertThat(violation.getAction()).isEqualTo(CommunityModerationAction.TEMPORARY_POSTING_BAN);
        assertThat(violation.getStatus()).isEqualTo(CommunityViolationStatus.ACTIVE);
        assertThat(violation.getExpireAt()).isEqualTo(expireAt);
    }

    private static AdminModerationRequest request(
            CommunityModerationAction action,
            UUID targetPostId,
            UUID targetUserId) {
        AdminModerationRequest request = new AdminModerationRequest();
        request.setAction(action);
        request.setTargetPostId(targetPostId);
        request.setTargetUserId(targetUserId);
        request.setReason("Reported abusive behavior");
        return request;
    }

    private static CommunityPost post(UUID ownerId, UUID postId) {
        return CommunityPost.builder()
                .id(postId)
                .bookingId(UUID.randomUUID())
                .ownerId(ownerId)
                .postType(CommunityPostType.LOOKING_OPPONENT)
                .status(CommunityPostStatus.OPEN)
                .title("Looking for opponent")
                .description("Friendly match")
                .skillLevel("INTERMEDIATE")
                .contactPhone("0900000000")
                .acceptedPlayersCount(0)
                .bookingCode("BK-001")
                .subFieldId(UUID.randomUUID())
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .build();
    }
}
