package com.project.common.events.notification;

public final class NotificationEventTopics {

    public static final String USER_REQUEST_OTP = "user.request-otp";
    public static final String BOOKING_CREATED = "booking.created";
    public static final String BOOKING_CONFIRMED = "booking.confirmed";
    public static final String BOOKING_CANCELLED = "booking.cancelled";
    public static final String BOOKING_COMPLETED = "booking.completed";
    public static final String PAYMENT_SUCCESS = "payment.success";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String USER_COMPLETED_BOOKING_COUNT_CHANGED = "user.completed-booking-count.changed";
    public static final String MODERATION_NOTIFICATION = "moderation.notification";
    public static final String PLATFORM_BAN_REQUESTED = "platform-ban.requested";
    public static final String USER_BALANCE_REFUND_REQUESTED = "user.balance.refund-requested";
    public static final String USER_BALANCE_DEDUCTION_REQUESTED = "user.balance.deduction-requested";
    public static final String COMMUNITY_NOTIFICATION = "community.notification";
    public static final String MATCH_EVALUATION_SUBMITTED = "match.evaluation.submitted";
    public static final String PLAYER_MATCH_STATISTICS_ADJUSTED = "player.match-statistics.adjusted";

    private NotificationEventTopics() {
    }
}
