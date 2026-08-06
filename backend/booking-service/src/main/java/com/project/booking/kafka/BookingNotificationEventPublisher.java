package com.project.booking.kafka;

import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.entity.Booking;
import com.project.common.events.notification.BookingCancelledEvent;
import com.project.common.events.notification.BookingConfirmedEvent;
import com.project.common.events.notification.BookingCreatedEvent;
import com.project.common.events.notification.CommunityNotificationEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.outbox.dto.OutboxSaveRequest;
import com.project.common.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingNotificationEventPublisher {

    private static final String RECURRING_WALLET_EMPTY_MESSAGE = "Đặt sân định kỳ của bạn đã được thanh toán thành công.\n"
            + "Số dư ví của bạn hiện đã về 0.\n"
            + "Vui lòng nạp tiền trước lần đặt sân định kỳ tiếp theo.";

    private static final String RECURRING_PAYMENT_FAILED_MESSAGE = "Thanh toán tự động cho đặt sân định kỳ của bạn thất bại do số dư ví không đủ.\n"
            + "Vui lòng nạp tiền và hoàn tất thanh toán trong vòng 30 phút.";

    private static final String RECURRING_PAUSED_MESSAGE = "Đặt sân định kỳ của bạn đã bị tạm dừng vì thanh toán không được hoàn tất trong vòng 30 phút.\n"
            + "Vui lòng nạp tiền và tiếp tục đặt sân định kỳ.";

    private final OutboxService outboxService;

    public void publishBookingCreated(Booking booking, SubFieldResponse subField, String userEmail) {
        save(booking, NotificationEventTopics.BOOKING_CREATED, new BookingCreatedEvent(
                        booking.getId(),
                        booking.getBookingCode(),
                        booking.getClientId(),
                        userEmail,
                        booking.getOwnerId(),
                        booking.getSubFieldId(),
                        subField.getFieldName(),
                        booking.getBookingDate(),
                        booking.getStartTime(),
                        booking.getEndTime(),
                        booking.getPlatformBookingFee(),
                        booking.getSubFieldPrice(),
                        booking.getBookingPrice(),
                        booking.getBookingType().name(),
                        null,
                        Instant.now()));
        log.info("Stored booking created notification outbox event: bookingId={}", booking.getId());
    }

    public void publishReservationChanged(Booking booking, SubFieldResponse subField, String action) {
        save(booking, NotificationEventTopics.BOOKING_CREATED, new BookingCreatedEvent(
                        booking.getId(),
                        booking.getBookingCode(),
                        booking.getClientId(),
                        null,
                        booking.getOwnerId(),
                        booking.getSubFieldId(),
                        subField.getFieldName(),
                        booking.getBookingDate(),
                        booking.getStartTime(),
                        booking.getEndTime(),
                        booking.getPlatformBookingFee(),
                        booking.getSubFieldPrice(),
                        booking.getBookingPrice(),
                        booking.getBookingType().name(),
                        action,
                        Instant.now()));
        log.info("Stored reservation {} notification outbox event: reservationId={}", action, booking.getId());
    }

    public void publishBookingCancelled(Booking booking, String userEmail) {
        save(booking, NotificationEventTopics.BOOKING_CANCELLED, new BookingCancelledEvent(
                        booking.getId(),
                        booking.getBookingCode(),
                        booking.getClientId(),
                        userEmail,
                        booking.getOwnerId(),
                        booking.getSubFieldId(),
                        null,
                        booking.getBookingDate(),
                        booking.getStartTime(),
                        booking.getEndTime(),
                        booking.getCancellationReason(),
                        booking.getCancelledBy() != null ? booking.getCancelledBy().name() : null,
                        booking.getBookingType().name(),
                        Instant.now()));
        log.info("Stored booking cancelled notification outbox event: bookingId={}", booking.getId());
    }

    public void publishBookingConfirmed(Booking booking, String userEmail) {
        save(booking, NotificationEventTopics.BOOKING_CONFIRMED, new BookingConfirmedEvent(
                        booking.getId(),
                        booking.getBookingCode(),
                        booking.getClientId(),
                        userEmail,
                        booking.getOwnerId(),
                        booking.getSubFieldId(),
                        null,
                        booking.getBookingDate(),
                        booking.getStartTime(),
                        booking.getEndTime(),
                        booking.getSubFieldPrice(),
                        booking.getBookingType().name(),
                        Instant.now()));
        log.info("Stored booking confirmed notification outbox event: bookingId={}", booking.getId());
    }

    public void publishRecurringPaymentWalletEmpty(Booking booking) {
        publishRecurringNotification(
                booking,
                "RECURRING_PAYMENT_WALLET_EMPTY",
                RECURRING_WALLET_EMPTY_MESSAGE);
    }

    public void publishRecurringPaymentFailed(Booking booking) {
        publishRecurringNotification(
                booking,
                "RECURRING_PAYMENT_FAILED",
                RECURRING_PAYMENT_FAILED_MESSAGE);
    }

    public void publishRecurringPausedPaymentTimeout(Booking booking) {
        publishRecurringNotification(
                booking,
                "RECURRING_PAUSED_PAYMENT_TIMEOUT",
                RECURRING_PAUSED_MESSAGE);
    }

    private void save(Booking booking, String topic, Object payload) {
        outboxService.save(new OutboxSaveRequest(
                "Booking",
                booking.getId().toString(),
                payload.getClass().getSimpleName(),
                topic,
                booking.getId().toString(),
                payload));
    }

    private void publishRecurringNotification(Booking booking, String code, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookingId", booking.getId());
        payload.put("bookingCode", booking.getBookingCode());
        payload.put("subFieldId", booking.getSubFieldId());
        payload.put("recurringBookingId", booking.getSourceRecurringBookingId());
        payload.put("bookingDate", booking.getBookingDate());
        payload.put("startTime", booking.getStartTime());
        payload.put("endTime", booking.getEndTime());
        payload.put("message", message);
        CommunityNotificationEvent event = new CommunityNotificationEvent(
                booking.getClientId(),
                null,
                code,
                message,
                payload,
                Instant.now());
        outboxService.save(new OutboxSaveRequest(
                "Booking",
                booking.getId().toString(),
                event.getClass().getSimpleName(),
                NotificationEventTopics.COMMUNITY_NOTIFICATION,
                booking.getId().toString(),
                event));
    }
}
