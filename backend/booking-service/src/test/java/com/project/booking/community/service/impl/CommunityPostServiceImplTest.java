package com.project.booking.community.service.impl;

import com.project.booking.community.dto.CommunityApplicationResponse;
import com.project.booking.community.entity.CommunityApplication;
import com.project.booking.community.entity.CommunityPost;
import com.project.booking.community.enums.CommunityApplicationStatus;
import com.project.booking.community.enums.CommunityPostStatus;
import com.project.booking.community.enums.CommunityPostType;
import com.project.booking.community.kafka.CommunityNotificationEventPublisher;
import com.project.booking.community.kafka.CommunityPostApplicationsHandlingEventPublisher;
import com.project.booking.community.kafka.MatchEvaluationEventPublisher;
import com.project.booking.community.mapper.CommunityMapper;
import com.project.booking.community.repository.CommunityApplicationRepository;
import com.project.booking.community.repository.CommunityPostRepository;
import com.project.booking.community.repository.MatchEvaluationRepository;
import com.project.booking.community.service.CommunityModerationService;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.MatchResultRepository;
import com.project.booking.repository.UserProjectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityPostServiceImplTest {

    @Mock
    private CommunityPostRepository postRepository;
    @Mock
    private CommunityApplicationRepository applicationRepository;
    @Mock
    private MatchEvaluationRepository evaluationRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Spy
    private CommunityMapper mapper;
    @Mock
    private CommunityNotificationEventPublisher notifications;
    @Mock
    private CommunityPostApplicationsHandlingEventPublisher applicationHandlingEvents;
    @Mock
    private MatchEvaluationEventPublisher evaluationEvents;
    @Mock
    private CommunityModerationService moderationService;
    @Mock
    private UserProjectionRepository userProjectionRepository;
    @Mock
    private MatchResultRepository matchResultRepository;

    @InjectMocks
    private CommunityPostServiceImpl service;

    @Test
    void acceptOpponentApplicationNotifiesOtherRejectedApplicants() {
        UUID ownerId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        UUID acceptedApplicationId = UUID.randomUUID();
        UUID acceptedApplicantId = UUID.randomUUID();
        UUID rejectedApplicationId = UUID.randomUUID();
        UUID rejectedApplicantId = UUID.randomUUID();

        CommunityPost post = CommunityPost.builder()
                .id(postId)
                .bookingId(UUID.randomUUID())
                .ownerId(ownerId)
                .postType(CommunityPostType.LOOKING_OPPONENT)
                .status(CommunityPostStatus.OPEN)
                .acceptedPlayersCount(0)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .build();
        CommunityApplication acceptedApplication = CommunityApplication.builder()
                .id(acceptedApplicationId)
                .post(post)
                .applicantId(acceptedApplicantId)
                .status(CommunityApplicationStatus.PENDING)
                .build();
        CommunityApplication rejectedApplication = CommunityApplication.builder()
                .id(rejectedApplicationId)
                .post(post)
                .applicantId(rejectedApplicantId)
                .status(CommunityApplicationStatus.PENDING)
                .build();

        when(postRepository.findPostOnlyById(postId)).thenReturn(Optional.of(post));
        when(applicationRepository.findByIdAndPostId(acceptedApplicationId, postId))
                .thenReturn(Optional.of(acceptedApplication));
        when(applicationRepository.findByPostIdAndStatus(postId, CommunityApplicationStatus.PENDING))
                .thenReturn(List.of(acceptedApplication, rejectedApplication));

        CommunityApplicationResponse response = service.accept(ownerId, postId, acceptedApplicationId);

        assertThat(response.getStatus()).isEqualTo(CommunityApplicationStatus.ACCEPTED);
        verify(applicationRepository).rejectOtherPendingApplications(
                postId,
                acceptedApplicationId,
                CommunityApplicationStatus.PENDING,
                CommunityApplicationStatus.REJECTED);
        verify(notifications).publish(
                eq(acceptedApplicantId),
                eq("COMMUNITY_OPPONENT_MATCHED"),
                eq("Doi cua ban da duoc chap nhan"),
                ArgumentMatchers.argThat(payload -> acceptedApplicationId.equals(payload.get("applicationId"))));
        verify(notifications).publish(
                eq(rejectedApplicantId),
                eq("COMMUNITY_APPLICATION_REJECTED"),
                eq("Yeu cau tham gia da bi tu choi"),
                ArgumentMatchers.argThat(payload -> rejectedApplicationId.equals(payload.get("applicationId"))));
    }

    @Test
    void closeRejectsPendingApplicationsButKeepsAcceptedApplications() {
        UUID ownerId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        CommunityPost post = openPlayerPost(ownerId, postId);

        when(postRepository.findPostOnlyById(postId)).thenReturn(Optional.of(post));

        var response = service.close(ownerId, postId);

        assertThat(response.getStatus()).isEqualTo(CommunityPostStatus.CLOSED);
        assertThat(response.getApplications()).isNull();
        verify(applicationHandlingEvents).publish(postId, "COMMUNITY_POST_CLOSED", "Bai dang da dong");
    }

    @Test
    void markFullRejectsPendingApplications() {
        UUID ownerId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        CommunityPost post = openPlayerPost(ownerId, postId);

        when(postRepository.findPostOnlyById(postId)).thenReturn(Optional.of(post));

        service.markFull(ownerId, postId);

        verify(applicationHandlingEvents).publish(postId, "COMMUNITY_PLAYER_RECRUITMENT_FULL", "Da tuyen du nguoi cho tran dau");
    }

    private static CommunityPost openPlayerPost(UUID ownerId, UUID postId) {
        return CommunityPost.builder()
                .id(postId)
                .bookingId(UUID.randomUUID())
                .ownerId(ownerId)
                .postType(CommunityPostType.LOOKING_PLAYER)
                .status(CommunityPostStatus.OPEN)
                .acceptedPlayersCount(1)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .build();
    }

    private static CommunityApplication application(
            CommunityPost post,
            UUID applicantId,
            CommunityApplicationStatus status) {
        return CommunityApplication.builder()
                .id(UUID.randomUUID())
                .post(post)
                .applicantId(applicantId)
                .status(status)
                .build();
    }
}
