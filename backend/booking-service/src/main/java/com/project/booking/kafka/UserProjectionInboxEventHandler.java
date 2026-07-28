package com.project.booking.kafka;

import com.project.booking.entity.UserProjection;
import com.project.booking.repository.UserProjectionRepository;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.PlayerMatchStatisticsAdjustedEvent;
import com.project.common.events.notification.UserCompletedBookingCountChangedEvent;
import com.project.common.events.notification.UserProfileUpdatedEvent;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.handler.InboxEventHandler;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserProjectionInboxEventHandler implements InboxEventHandler {
    private final InboxService inboxService;
    private final UserProjectionRepository repository;

    @Override
    public boolean supports(String topic) {
        return NotificationEventTopics.USER_COMPLETED_BOOKING_COUNT_CHANGED.equals(topic)
                || NotificationEventTopics.USER_PROFILE_UPDATED.equals(topic)
                || NotificationEventTopics.PLAYER_MATCH_STATISTICS_ADJUSTED.equals(topic);
    }

    @Override
    @Transactional
    public void handle(InboxEvent event) {
        if (NotificationEventTopics.PLAYER_MATCH_STATISTICS_ADJUSTED.equals(event.getTopic())) {
            adjustMatchStatistics(inboxService.payload(event, PlayerMatchStatisticsAdjustedEvent.class));
            return;
        }
        if (NotificationEventTopics.USER_PROFILE_UPDATED.equals(event.getTopic())) {
            updateProfile(inboxService.payload(event, UserProfileUpdatedEvent.class));
            return;
        }
        UserCompletedBookingCountChangedEvent payload = inboxService.payload(event, UserCompletedBookingCountChangedEvent.class);
        UserProjection projection = repository.findById(payload.userId())
                .orElseGet(() -> UserProjection.builder().userId(payload.userId()).build());
        projection.setCompletedBookingCount(payload.completedBookingCount());
        repository.save(projection);
    }

    private void updateProfile(UserProfileUpdatedEvent payload) {
        UserProjection projection = repository.findById(payload.userId())
                .orElseGet(() -> UserProjection.builder().userId(payload.userId()).build());
        projection.setEmail(payload.email());
        projection.setFullName(payload.fullName());
        projection.setPhoneNumber(payload.phoneNumber());
        projection.setAvatarUrl(payload.avatarUrl());
        projection.setBio(payload.bio());
        projection.setTeamPhotoUrl(payload.teamPhotoUrl());
        projection.setSkillLevel(payload.skillLevel());
        projection.setUserType(payload.userType());
        projection.setStatus(payload.status());
        projection.setBalance(payload.balance() == null ? 0L : payload.balance());
        projection.setTotalMatches(valueOrZero(payload.totalMatches()));
        projection.setWins(valueOrZero(payload.wins()));
        projection.setLosses(valueOrZero(payload.losses()));
        projection.setDraws(valueOrZero(payload.draws()));
        projection.setNoCancelRate(payload.noCancelRate() == null ? projection.getNoCancelRate() : payload.noCancelRate());
        projection.setOnTimeRate(payload.onTimeRate() == null ? projection.getOnTimeRate() : payload.onTimeRate());
        projection.setFairPlayRate(payload.fairPlayRate() == null ? projection.getFairPlayRate() : payload.fairPlayRate());
        projection.setCompletedBookingCount(valueOrZero(payload.completedBookingCount()));
        repository.save(projection);
    }

    private void adjustMatchStatistics(PlayerMatchStatisticsAdjustedEvent payload) {
        UserProjection projection = repository.findById(payload.userId())
                .orElseGet(() -> UserProjection.builder().userId(payload.userId()).build());
        projection.setTotalMatches(nonNegative(valueOrZero(projection.getTotalMatches()) + payload.totalMatchesDelta()));
        projection.setWins(nonNegative(valueOrZero(projection.getWins()) + payload.winsDelta()));
        projection.setLosses(nonNegative(valueOrZero(projection.getLosses()) + payload.lossesDelta()));
        projection.setDraws(nonNegative(valueOrZero(projection.getDraws()) + payload.drawsDelta()));
        repository.save(projection);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private int nonNegative(int value) {
        return Math.max(0, value);
    }
}
