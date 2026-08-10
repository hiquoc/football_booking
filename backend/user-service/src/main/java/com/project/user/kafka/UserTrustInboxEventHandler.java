package com.project.user.kafka;

import com.project.common.events.notification.BookingCompletedEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.PlatformBanRequestedEvent;
import com.project.common.events.notification.PlayerMatchStatisticsAdjustedEvent;
import com.project.common.events.notification.UserCompletedBookingCountChangedEvent;
import com.project.common.exception.BadRequestException;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.handler.InboxEventHandler;
import com.project.common.inbox.service.InboxService;
import com.project.user.entity.User;
import com.project.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserTrustInboxEventHandler implements InboxEventHandler {
    private static final Set<String> TOPICS = Set.of(
            NotificationEventTopics.BOOKING_COMPLETED,
            NotificationEventTopics.PLATFORM_BAN_REQUESTED,
            NotificationEventTopics.PLAYER_MATCH_STATISTICS_ADJUSTED);
    private static final String PLATFORM_BANNED_STATUS = "PLATFORM_BANNED";

    private final InboxService inboxService;
    private final UserRepository userRepository;
    private final UserTrustEventPublisher publisher;
    private final UserProfileEventPublisher userProfileEventPublisher;
    private final UserNotificationEventPublisher userNotificationEventPublisher;

    @Override
    public boolean supports(String topic) {
        return TOPICS.contains(topic);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "user-by-id", allEntries = true)
    public void handle(InboxEvent event) {
        if (NotificationEventTopics.BOOKING_COMPLETED.equals(event.getTopic())) {
            incrementCompletedBookingCount(inboxService.payload(event, BookingCompletedEvent.class));
            return;
        }
        if (NotificationEventTopics.PLAYER_MATCH_STATISTICS_ADJUSTED.equals(event.getTopic())) {
            adjustPlayerMatchStatistics(inboxService.payload(event, PlayerMatchStatisticsAdjustedEvent.class));
            return;
        }
        platformBan(inboxService.payload(event, PlatformBanRequestedEvent.class));
    }

    private void incrementCompletedBookingCount(BookingCompletedEvent event) {
        User user = userRepository.findForUpdateById(event.userId())
                .orElseThrow(() -> new BadRequestException("User not found"));
        int nextCount = (user.getCompletedBookingCount() == null ? 0 : user.getCompletedBookingCount()) + 1;
        user.setCompletedBookingCount(nextCount);
        publisher.publishCompletedBookingCountChanged(new UserCompletedBookingCountChangedEvent(
                user.getId(), nextCount, Instant.now()));
    }

    private void platformBan(PlatformBanRequestedEvent event) {
        User user = userRepository.findForUpdateById(event.userId())
                .orElseThrow(() -> new BadRequestException("User not found"));
        String previousStatus = user.getStatus();
        user.setStatus(PLATFORM_BANNED_STATUS);
        userProfileEventPublisher.publishUpdated(user);
        if (!PLATFORM_BANNED_STATUS.equals(previousStatus)) {
            userNotificationEventPublisher.publishModerationNotification(
                    user.getId(),
                    "PLATFORM_BAN",
                    "Tài khoản của bạn đã bị cấm",
                    java.util.Map.of(
                            "reason", event.reason(),
                            "requestedBy", event.requestedBy(),
                            "source", event.source()));
        }
    }

    private void adjustPlayerMatchStatistics(PlayerMatchStatisticsAdjustedEvent event) {
        User user = userRepository.findForUpdateById(event.userId())
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setTotalMatches(nonNegative(valueOrZero(user.getTotalMatches()) + event.totalMatchesDelta()));
        user.setWins(nonNegative(valueOrZero(user.getWins()) + event.winsDelta()));
        user.setLosses(nonNegative(valueOrZero(user.getLosses()) + event.lossesDelta()));
        user.setDraws(nonNegative(valueOrZero(user.getDraws()) + event.drawsDelta()));
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private int nonNegative(int value) {
        return Math.max(0, value);
    }
}
