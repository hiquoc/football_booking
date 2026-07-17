package com.project.notification.kafka;

import com.project.common.events.notification.BookingCancelledEvent;
import com.project.common.events.notification.BookingConfirmedEvent;
import com.project.common.events.notification.BookingCreatedEvent;
import com.project.common.events.notification.CommunityNotificationEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.PaymentFailedEvent;
import com.project.common.events.notification.PaymentSuccessEvent;
import com.project.common.events.notification.UserRequestOtpEvent;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final InboxService inboxService;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @KafkaListener(topics = NotificationEventTopics.USER_REQUEST_OTP, groupId = "${spring.kafka.consumer.group-id}")
    public void onUserRequestOtp(ConsumerRecord<String, UserRequestOtpEvent> record) {
        inboxService.receive(record, consumerGroup);
    }

    @KafkaListener(topics = NotificationEventTopics.BOOKING_CREATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onBookingCreated(ConsumerRecord<String, BookingCreatedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }

    @KafkaListener(topics = NotificationEventTopics.BOOKING_CONFIRMED, groupId = "${spring.kafka.consumer.group-id}")
    public void onBookingConfirmed(ConsumerRecord<String, BookingConfirmedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }

    @KafkaListener(topics = NotificationEventTopics.BOOKING_CANCELLED, groupId = "${spring.kafka.consumer.group-id}")
    public void onBookingCancelled(ConsumerRecord<String, BookingCancelledEvent> record) {
        inboxService.receive(record, consumerGroup);
    }

    @KafkaListener(topics = NotificationEventTopics.PAYMENT_SUCCESS, groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentSuccess(ConsumerRecord<String, PaymentSuccessEvent> record) {
        inboxService.receive(record, consumerGroup);
    }

    @KafkaListener(topics = NotificationEventTopics.PAYMENT_FAILED, groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentFailed(ConsumerRecord<String, PaymentFailedEvent> record) {
        inboxService.receive(record, consumerGroup);
    }

    @KafkaListener(topics = NotificationEventTopics.COMMUNITY_NOTIFICATION, groupId = "${spring.kafka.consumer.group-id}")
    public void onCommunityNotification(ConsumerRecord<String, CommunityNotificationEvent> record) {
        inboxService.receive(record, consumerGroup);
    }
}
