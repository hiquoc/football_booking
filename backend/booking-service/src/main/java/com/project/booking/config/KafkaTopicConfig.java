package com.project.booking.config;

import com.project.booking.events.RecurringBookingEventTopics;
import com.project.common.events.field.FieldEventTopics;
import com.project.common.events.notification.NotificationEventTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    private static final int PARTITIONS = 3;
    private static final int REPLICAS = 1;

    @Bean
    public NewTopic subFieldCreatedTopic() {
        return topic(FieldEventTopics.SUB_FIELD_CREATED);
    }

    @Bean
    public NewTopic subFieldUpdatedTopic() {
        return topic(FieldEventTopics.SUB_FIELD_UPDATED);
    }

    @Bean
    public NewTopic subFieldDeletedTopic() {
        return topic(FieldEventTopics.SUB_FIELD_DELETED);
    }

    @Bean
    public NewTopic fieldOperatingHoursUpdatedTopic() {
        return topic(FieldEventTopics.FIELD_OPERATING_HOURS_UPDATED);
    }

    @Bean
    public NewTopic subFieldOperatingHoursUpdatedTopic() {
        return topic(FieldEventTopics.SUB_FIELD_OPERATING_HOURS_UPDATED);
    }

    @Bean
    public NewTopic fieldClosureCreatedTopic() {
        return topic(FieldEventTopics.FIELD_CLOSURE_CREATED);
    }

    @Bean
    public NewTopic fieldClosureUpdatedTopic() {
        return topic(FieldEventTopics.FIELD_CLOSURE_UPDATED);
    }

    @Bean
    public NewTopic fieldClosureDeletedTopic() {
        return topic(FieldEventTopics.FIELD_CLOSURE_DELETED);
    }

    @Bean
    public NewTopic communityNotificationTopic() {
        return topic(NotificationEventTopics.COMMUNITY_NOTIFICATION);
    }

    @Bean
    public NewTopic bookingCompletedTopic() {
        return topic(NotificationEventTopics.BOOKING_COMPLETED);
    }

    @Bean
    public NewTopic bookingCancelledTopic() {
        return topic(NotificationEventTopics.BOOKING_CANCELLED);
    }

    @Bean
    public NewTopic userCompletedBookingCountChangedTopic() {
        return topic(NotificationEventTopics.USER_COMPLETED_BOOKING_COUNT_CHANGED);
    }

    @Bean
    public NewTopic userProfileUpdatedTopic() {
        return topic(NotificationEventTopics.USER_PROFILE_UPDATED);
    }

    @Bean
    public NewTopic moderationNotificationTopic() {
        return topic(NotificationEventTopics.MODERATION_NOTIFICATION);
    }

    @Bean
    public NewTopic platformBanRequestedTopic() {
        return topic(NotificationEventTopics.PLATFORM_BAN_REQUESTED);
    }

    @Bean
    public NewTopic platformBanClearedTopic() {
        return topic(NotificationEventTopics.PLATFORM_BAN_CLEARED);
    }

    @Bean
    public NewTopic matchEvaluationSubmittedTopic() {
        return topic(NotificationEventTopics.MATCH_EVALUATION_SUBMITTED);
    }

    @Bean
    public NewTopic recurringOccurrenceRequestedTopic() {
        return topic(RecurringBookingEventTopics.RECURRING_OCCURRENCE_REQUESTED);
    }

    @Bean
    public NewTopic playerMatchStatisticsAdjustedTopic() {
        return topic(NotificationEventTopics.PLAYER_MATCH_STATISTICS_ADJUSTED);
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name)
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .build();
    }
}
