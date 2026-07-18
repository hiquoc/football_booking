package com.project.booking.kafka;

import com.project.booking.entity.UserReplica;
import com.project.booking.repository.UserReplicaRepository;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.PlayerMatchStatisticsAdjustedEvent;
import com.project.common.events.notification.UserCompletedBookingCountChangedEvent;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.handler.InboxEventHandler;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserReplicaInboxEventHandler implements InboxEventHandler {
    private final InboxService inboxService;
    private final UserReplicaRepository repository;

    @Override
    public boolean supports(String topic) {
        return NotificationEventTopics.USER_COMPLETED_BOOKING_COUNT_CHANGED.equals(topic)
                || NotificationEventTopics.PLAYER_MATCH_STATISTICS_ADJUSTED.equals(topic);
    }

    @Override
    @Transactional
    public void handle(InboxEvent event) {
        if (NotificationEventTopics.PLAYER_MATCH_STATISTICS_ADJUSTED.equals(event.getTopic())) {
            adjustMatchStatistics(inboxService.payload(event, PlayerMatchStatisticsAdjustedEvent.class));
            return;
        }
        UserCompletedBookingCountChangedEvent payload = inboxService.payload(event, UserCompletedBookingCountChangedEvent.class);
        UserReplica replica = repository.findById(payload.userId())
                .orElseGet(() -> UserReplica.builder().userId(payload.userId()).build());
        replica.setCompletedBookingCount(payload.completedBookingCount());
        repository.save(replica);
    }

    private void adjustMatchStatistics(PlayerMatchStatisticsAdjustedEvent payload) {
        UserReplica replica = repository.findById(payload.userId())
                .orElseGet(() -> UserReplica.builder().userId(payload.userId()).build());
        replica.setTotalMatches(nonNegative(valueOrZero(replica.getTotalMatches()) + payload.totalMatchesDelta()));
        replica.setWins(nonNegative(valueOrZero(replica.getWins()) + payload.winsDelta()));
        replica.setLosses(nonNegative(valueOrZero(replica.getLosses()) + payload.lossesDelta()));
        replica.setDraws(nonNegative(valueOrZero(replica.getDraws()) + payload.drawsDelta()));
        repository.save(replica);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private int nonNegative(int value) {
        return Math.max(0, value);
    }
}
