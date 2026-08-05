package com.project.booking.community.service.impl;

import com.project.booking.community.entity.CommunityApplication;
import com.project.booking.community.entity.CommunityPost;
import com.project.booking.community.enums.CommunityApplicationStatus;
import com.project.booking.community.enums.CommunityPostStatus;
import com.project.booking.community.enums.CommunityPostType;
import com.project.booking.community.kafka.CommunityPostApplicationsHandlingEventPublisher;
import com.project.booking.community.kafka.CommunityNotificationEventPublisher;
import com.project.booking.community.repository.CommunityApplicationRepository;
import com.project.booking.community.repository.CommunityPostRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityPostApplicationHandlingServiceImplTest {

    @Mock
    private CommunityPostRepository postRepository;
    @Mock
    private CommunityApplicationRepository applicationRepository;
    @Mock
    private CommunityNotificationEventPublisher notifications;
    @Mock
    private CommunityPostApplicationsHandlingEventPublisher continuationEvents;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private CommunityPostApplicationHandlingServiceImpl service;

    @Test
    void acceptedPhaseNotifiesAcceptedApplicationsAndEnqueuesPendingPhase() {
        UUID postId = UUID.randomUUID();
        CommunityPost post = post(postId);
        CommunityApplication acceptedApplication = application(post, CommunityApplicationStatus.ACCEPTED);

        when(postRepository.findPostOnlyById(postId)).thenReturn(Optional.of(post));
        when(applicationRepository.findByPostIdAndStatus(eq(postId), eq(CommunityApplicationStatus.ACCEPTED), any()))
                .thenReturn(List.of(acceptedApplication));

        service.handlePostClosed(postId, "COMMUNITY_POST_CLOSED", "Bai dang da dong", "ACCEPTED", 0);

        verify(notifications).publish(
                eq(acceptedApplication.getApplicantId()),
                eq("COMMUNITY_POST_CLOSED"),
                eq("Bai dang da dong"),
                ArgumentMatchers.argThat(payload -> acceptedApplication.getId().equals(payload.get("applicationId"))));
        verify(continuationEvents).publish(postId, "COMMUNITY_POST_CLOSED", "Bai dang da dong", "PENDING", 0);
        verify(entityManager).detach(acceptedApplication);
    }

    @Test
    void pendingPhaseRejectsOnePendingApplicationBatch() {
        UUID postId = UUID.randomUUID();
        CommunityPost post = post(postId);
        CommunityApplication pendingApplication = application(post, CommunityApplicationStatus.PENDING);

        when(postRepository.findPostOnlyById(postId)).thenReturn(Optional.of(post));
        when(applicationRepository.findByPostIdAndStatus(eq(postId), eq(CommunityApplicationStatus.PENDING), any()))
                .thenReturn(List.of(pendingApplication));

        service.handlePostClosed(postId, "COMMUNITY_POST_CLOSED", "Bai dang da dong", "PENDING", 0);

        verify(notifications).publish(
                eq(pendingApplication.getApplicantId()),
                eq("COMMUNITY_POST_CLOSED"),
                eq("Bai dang da dong"),
                ArgumentMatchers.argThat(payload -> pendingApplication.getId().equals(payload.get("applicationId"))));
        verify(applicationRepository).rejectPendingApplicationsById(
                List.of(pendingApplication.getId()),
                CommunityApplicationStatus.PENDING,
                CommunityApplicationStatus.REJECTED);
        verify(entityManager).detach(pendingApplication);
    }

    private static CommunityPost post(UUID postId) {
        return CommunityPost.builder()
                .id(postId)
                .bookingId(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .postType(CommunityPostType.LOOKING_PLAYER)
                .status(CommunityPostStatus.CLOSED)
                .acceptedPlayersCount(1)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 0))
                .build();
    }

    private static CommunityApplication application(CommunityPost post, CommunityApplicationStatus status) {
        return CommunityApplication.builder()
                .id(UUID.randomUUID())
                .post(post)
                .applicantId(UUID.randomUUID())
                .status(status)
                .build();
    }
}
