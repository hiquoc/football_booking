package com.project.booking.kafka;

import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.PlayerMatchStatisticsAdjustedEvent;
import com.project.common.outbox.dto.OutboxSaveRequest;
import com.project.common.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MatchResultEventPublisher {
    private final OutboxService outboxService;

    public void publish(PlayerMatchStatisticsAdjustedEvent event) {
        outboxService.save(new OutboxSaveRequest(
                "User",
                event.userId().toString(),
                event.getClass().getSimpleName(),
                NotificationEventTopics.PLAYER_MATCH_STATISTICS_ADJUSTED,
                event.userId().toString(),
                event));
    }
}
