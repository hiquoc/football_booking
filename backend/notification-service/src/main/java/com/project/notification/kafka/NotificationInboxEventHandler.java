package com.project.notification.kafka;

import com.project.common.events.notification.BookingCancelledEvent;
import com.project.common.events.notification.BookingConfirmedEvent;
import com.project.common.events.notification.BookingCreatedEvent;
import com.project.common.events.notification.CommunityNotificationEvent;
import com.project.common.events.notification.ModerationNotificationEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.PaymentFailedEvent;
import com.project.common.events.notification.PaymentSuccessEvent;
import com.project.common.events.notification.UserBalanceUpdatedEvent;
import com.project.common.events.notification.UserRequestOtpEvent;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.handler.InboxEventHandler;
import com.project.common.inbox.service.InboxService;
import com.project.notification.dto.NotificationRequest;
import com.project.notification.dto.UserBalanceUpdateMessage;
import com.project.notification.enums.NotificationChannel;
import com.project.notification.enums.NotificationCode;
import com.project.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationInboxEventHandler implements InboxEventHandler {

    private static final Set<String> TOPICS = Set.of(
            NotificationEventTopics.USER_REQUEST_OTP,
            NotificationEventTopics.BOOKING_CREATED,
            NotificationEventTopics.BOOKING_CONFIRMED,
            NotificationEventTopics.BOOKING_CANCELLED,
            NotificationEventTopics.PAYMENT_SUCCESS,
            NotificationEventTopics.PAYMENT_FAILED,
            NotificationEventTopics.USER_BALANCE_UPDATED,
            NotificationEventTopics.COMMUNITY_NOTIFICATION,
            NotificationEventTopics.MODERATION_NOTIFICATION);

    private final InboxService inboxService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public boolean supports(String topic) {
        return TOPICS.contains(topic);
    }

    @Override
    @Transactional
    public void handle(InboxEvent event) {
        switch (event.getTopic()) {
            case NotificationEventTopics.USER_REQUEST_OTP ->
                    handleUserRequestOtp(inboxService.payload(event, UserRequestOtpEvent.class));
            case NotificationEventTopics.BOOKING_CREATED ->
                    handleBookingCreated(inboxService.payload(event, BookingCreatedEvent.class));
            case NotificationEventTopics.BOOKING_CONFIRMED ->
                    handleBookingConfirmed(inboxService.payload(event, BookingConfirmedEvent.class));
            case NotificationEventTopics.BOOKING_CANCELLED ->
                    handleBookingCancelled(inboxService.payload(event, BookingCancelledEvent.class));
            case NotificationEventTopics.PAYMENT_SUCCESS ->
                    handlePaymentSuccess(inboxService.payload(event, PaymentSuccessEvent.class));
            case NotificationEventTopics.PAYMENT_FAILED ->
                    handlePaymentFailed(inboxService.payload(event, PaymentFailedEvent.class));
            case NotificationEventTopics.USER_BALANCE_UPDATED ->
                    handleUserBalanceUpdated(inboxService.payload(event, UserBalanceUpdatedEvent.class));
            case NotificationEventTopics.COMMUNITY_NOTIFICATION ->
                    handleCommunityNotification(inboxService.payload(event, CommunityNotificationEvent.class));
            case NotificationEventTopics.MODERATION_NOTIFICATION ->
                    handleModerationNotification(inboxService.payload(event, ModerationNotificationEvent.class));
            default -> throw new IllegalStateException("Unsupported topic " + event.getTopic());
        }
    }

    private void handleUserRequestOtp(UserRequestOtpEvent event) {
        log.info("Received user request OTP event: phoneNumber={}", event.phoneNumber());
    }

    private void handleBookingCreated(BookingCreatedEvent event) {
        notificationService.create(NotificationRequest.builder()
                .userId(event.userId())
                .recipientEmail(event.userEmail())
                .code(NotificationCode.BOOKING_CREATED)
                .title("Đã tạo yêu cầu đặt sân")
                .payload(bookingPayload(event.bookingId(), event.bookingCode(), event.subFieldId(), event.fieldName(),
                        event.bookingDate(), event.startTime(), event.endTime(), event.totalAmount()))
                .channels(List.of(NotificationChannel.IN_APP))
                .build());
    }

    private void handleBookingConfirmed(BookingConfirmedEvent event) {
        notificationService.create(NotificationRequest.builder()
                .userId(event.userId())
                .recipientEmail(event.userEmail())
                .code(NotificationCode.BOOKING_CONFIRMED)
                .title("Đặt sân đã được xác nhận")
                .payload(bookingPayload(event.bookingId(), event.bookingCode(), event.subFieldId(), event.fieldName(),
                        event.bookingDate(), event.startTime(), event.endTime(), event.totalAmount()))
                .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                .build());
    }

    private void handleBookingCancelled(BookingCancelledEvent event) {
        Map<String, Object> payload = bookingPayload(event.bookingId(), event.bookingCode(), event.subFieldId(),
                event.fieldName(), event.bookingDate(), event.startTime(), event.endTime(), null);
        payload.put("reason", event.reason());
        payload.put("cancelledBy", event.cancelledBy());
        notificationService.create(NotificationRequest.builder()
                .userId(event.userId())
                .recipientEmail(event.userEmail())
                .code(NotificationCode.BOOKING_CANCELLED)
                .title("Đặt sân đã bị hủy")
                .payload(payload)
                .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                .build());
    }

    private void handlePaymentSuccess(PaymentSuccessEvent event) {
        notificationService.create(NotificationRequest.builder()
                .userId(event.userId())
                .recipientEmail(event.userEmail())
                .code(NotificationCode.PAYMENT_SUCCESS)
                .title("Thanh toán thành công")
                .payload(Map.of(
                        "paymentId", event.paymentId(),
                        "bookingId", event.bookingId(),
                        "bookingCode", event.bookingCode(),
                        "amount", event.amount()))
                .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                .build());
    }

    private void handlePaymentFailed(PaymentFailedEvent event) {
        notificationService.create(NotificationRequest.builder()
                .userId(event.userId())
                .recipientEmail(event.userEmail())
                .code(NotificationCode.PAYMENT_FAILED)
                .title("Thanh toán thất bại")
                .payload(Map.of(
                        "paymentId", event.paymentId(),
                        "bookingId", event.bookingId(),
                        "bookingCode", event.bookingCode(),
                        "amount", event.amount(),
                        "reason", event.reason()))
                .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                .build());
    }

    private void handleUserBalanceUpdated(UserBalanceUpdatedEvent event) {
        messagingTemplate.convertAndSendToUser(
                event.userId().toString(),
                "/queue/balance",
                new UserBalanceUpdateMessage(event.userId(), event.balance(), event.reason(), event.occurredAt()));
    }

    private void handleCommunityNotification(CommunityNotificationEvent event) {
        notificationService.create(NotificationRequest.builder()
                .userId(event.userId())
                .recipientEmail(event.userEmail())
                .code(NotificationCode.valueOf(event.code()))
                .title(event.title())
                .payload(event.payload())
                .channels(List.of(NotificationChannel.IN_APP))
                .build());
    }

    private void handleModerationNotification(ModerationNotificationEvent event) {
        notificationService.create(NotificationRequest.builder()
                .userId(event.userId())
                .recipientEmail(event.userEmail())
                .code(NotificationCode.valueOf(event.code()))
                .title(event.title())
                .payload(event.payload())
                .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                .build());
    }

    private Map<String, Object> bookingPayload(Object bookingId, String bookingCode, Object subFieldId,
                                                String fieldName, Object bookingDate, Object startTime,
                                                Object endTime, Object totalAmount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookingId", bookingId);
        payload.put("bookingCode", bookingCode);
        payload.put("subFieldId", subFieldId);
        payload.put("fieldName", fieldName);
        payload.put("bookingDate", bookingDate);
        payload.put("startTime", startTime);
        payload.put("endTime", endTime);
        if (totalAmount != null) {
            payload.put("totalAmount", totalAmount);
        }
        return payload;
    }
}
