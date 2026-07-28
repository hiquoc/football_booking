package com.project.user.kafka;

import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.UserProfileUpdatedEvent;
import com.project.common.outbox.dto.OutboxSaveRequest;
import com.project.common.outbox.service.OutboxService;
import com.project.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class UserProfileEventPublisher {
    private final OutboxService outboxService;

    public void publishUpdated(User user) {
        UserProfileUpdatedEvent event = new UserProfileUpdatedEvent(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getTeamPhotoUrl(),
                user.getSkillLevel() == null ? null : user.getSkillLevel().name(),
                user.getUserType() == null ? null : user.getUserType().name(),
                user.getStatus(),
                user.getBalance(),
                user.getTotalMatches(),
                user.getWins(),
                user.getDraws(),
                user.getLosses(),
                user.getNoCancelRate(),
                user.getOnTimeRate(),
                user.getFairPlayRate(),
                user.getCompletedBookingCount(),
                Instant.now());
        outboxService.save(new OutboxSaveRequest(
                "UserProfile",
                user.getId().toString(),
                event.getClass().getSimpleName(),
                NotificationEventTopics.USER_PROFILE_UPDATED,
                user.getId().toString(),
                event));
    }
}
