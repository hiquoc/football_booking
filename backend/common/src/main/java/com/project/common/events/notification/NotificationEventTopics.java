package com.project.common.events.notification;

public final class NotificationEventTopics {

    public static final String USER_REQUEST_OTP = "user.request-otp";
    public static final String BOOKING_CREATED = "booking.created";
    public static final String BOOKING_CONFIRMED = "booking.confirmed";
    public static final String BOOKING_CANCELLED = "booking.cancelled";
    public static final String PAYMENT_SUCCESS = "payment.success";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String USER_BALANCE_REFUND_REQUESTED = "user.balance.refund-requested";
    public static final String USER_BALANCE_DEDUCTION_REQUESTED = "user.balance.deduction-requested";
    public static final String COMMUNITY_NOTIFICATION = "community.notification";

    private NotificationEventTopics() {
    }
}
