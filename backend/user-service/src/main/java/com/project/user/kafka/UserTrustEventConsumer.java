package com.project.user.kafka;

import com.project.common.events.notification.BookingCompletedEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.PlatformBanRequestedEvent;
import com.project.common.events.notification.PlayerMatchStatisticsAdjustedEvent;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserTrustEventConsumer {
    private final InboxService inboxService;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @KafkaListener(topics = NotificationEventTopics.BOOKING_COMPLETED, groupId = "${spring.kafka.consumer.group-id}")
    public void onBookingCompleted(ConsumerRecord<String, BookingCompletedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }

    @KafkaListener(topics = NotificationEventTopics.PLATFORM_BAN_REQUESTED, groupId = "${spring.kafka.consumer.group-id}")
    public void onPlatformBanRequested(ConsumerRecord<String, PlatformBanRequestedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }

    @KafkaListener(topics = NotificationEventTopics.PLAYER_MATCH_STATISTICS_ADJUSTED, groupId = "${spring.kafka.consumer.group-id}")
    public void onPlayerMatchStatisticsAdjusted(ConsumerRecord<String, PlayerMatchStatisticsAdjustedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }
}
