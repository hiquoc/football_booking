package com.project.booking.community.service.impl;

import com.project.booking.community.entity.CommunityApplication;
import com.project.booking.community.entity.CommunityPost;
import com.project.booking.community.enums.CommunityApplicationStatus;
import com.project.booking.community.kafka.CommunityPostApplicationsHandlingEventPublisher;
import com.project.booking.community.kafka.CommunityNotificationEventPublisher;
import com.project.booking.community.repository.CommunityApplicationRepository;
import com.project.booking.community.repository.CommunityPostRepository;
import com.project.booking.community.service.CommunityPostApplicationHandlingService;
import com.project.common.exception.BadRequestException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityPostApplicationHandlingServiceImpl implements CommunityPostApplicationHandlingService {
    private static final int APPLICATION_BATCH_SIZE = 100;

    private final CommunityPostRepository postRepository;
    private final CommunityApplicationRepository applicationRepository;
    private final CommunityNotificationEventPublisher notifications;
    private final CommunityPostApplicationsHandlingEventPublisher continuationEvents;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void handlePostClosed(UUID postId, String notificationCode, String notificationTitle, String phase, int page) {
        CommunityPost post = postRepository.findPostOnlyById(postId)
                .orElseThrow(() -> new BadRequestException("Community post not found"));
        if ("ACCEPTED".equalsIgnoreCase(phase)) {
            notifyAcceptedApplicationsBatch(post, notificationCode, notificationTitle, page);
            return;
        }
        if ("PENDING".equalsIgnoreCase(phase)) {
            rejectPendingApplicationsBatch(post, notificationCode, notificationTitle);
            return;
        }
        throw new BadRequestException("Unsupported community application handling phase: " + phase);
    }

    private void notifyAcceptedApplicationsBatch(CommunityPost post, String code, String title, int page) {
        List<CommunityApplication> applications = applicationRepository.findByPostIdAndStatus(
                post.getId(),
                CommunityApplicationStatus.ACCEPTED,
                PageRequest.of(page, APPLICATION_BATCH_SIZE));
        applications.forEach(application -> {
            notifications.publish(application.getApplicantId(), code, title,
                    payload(post, Map.of("applicationId", application.getId())));
            entityManager.detach(application);
        });
        if (applications.size() == APPLICATION_BATCH_SIZE) {
            continuationEvents.publish(post.getId(), code, title, "ACCEPTED", page + 1);
        } else {
            continuationEvents.publish(post.getId(), code, title, "PENDING", 0);
        }
    }

    private void rejectPendingApplicationsBatch(CommunityPost post, String code, String title) {
        List<CommunityApplication> applications = applicationRepository.findByPostIdAndStatus(
                post.getId(),
                CommunityApplicationStatus.PENDING,
                PageRequest.of(0, APPLICATION_BATCH_SIZE));
        if (applications.isEmpty()) {
            return;
        }
        applications.forEach(application -> notifications.publish(application.getApplicantId(), code, title,
                payload(post, Map.of("applicationId", application.getId()))));
        applicationRepository.rejectPendingApplicationsById(
                applications.stream().map(CommunityApplication::getId).toList(),
                CommunityApplicationStatus.PENDING,
                CommunityApplicationStatus.REJECTED);
        applications.forEach(entityManager::detach);
        if (applications.size() == APPLICATION_BATCH_SIZE) {
            continuationEvents.publish(post.getId(), code, title, "PENDING", 0);
        }
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
}
