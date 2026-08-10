package com.project.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final String developerMessage;

    public BusinessException(String message, HttpStatus status) {
        this(message, status, null);
    }

    public BusinessException(String message, HttpStatus status, String code) {
        this(message, status, code, null);
    }

    public BusinessException(String message, HttpStatus status, String code, String developerMessage) {
        super(message);
        this.status = status;
        this.code = code != null ? code : defaultCode(status);
        this.developerMessage = developerMessage != null ? developerMessage : defaultDeveloperMessage(this.code, status);
    }

    private static String defaultCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "INVALID_REQUEST";
            case UNAUTHORIZED -> "UNAUTHORIZED";
            case FORBIDDEN -> "FORBIDDEN";
            case NOT_FOUND -> "RESOURCE_NOT_FOUND";
            case CONFLICT -> "CONFLICT";
            case TOO_MANY_REQUESTS -> "RATE_LIMITED";
            case SERVICE_UNAVAILABLE -> "SERVICE_UNAVAILABLE";
            default -> status.is5xxServerError() ? "INTERNAL_ERROR" : "INVALID_REQUEST";
        };
    }

    private static String defaultDeveloperMessage(String code, HttpStatus status) {
        return switch (code) {
            case "FIELD_NOT_FOUND" -> "Field not found.";
            case "SUBFIELD_NOT_FOUND" -> "Sub-field not found.";
            case "FIELD_TYPE_NOT_FOUND" -> "Field type not found.";
            case "REVIEW_COMPLETED_BOOKING_REQUIRED" -> "A completed booking at this field is required before reviewing.";
            case "TIME_PRICE_RULES_OPERATING_HOURS_COVERAGE_REQUIRED" -> "Time price rules must cover all field operating hours.";
            case "BOOKING_NOT_FOUND" -> "Booking not found.";
            case "BOOKING_NOT_AVAILABLE" -> "Booking time is not available.";
            case "BOOKING_ALREADY_EXISTS" -> "Booking already exists.";
            case "BOOKING_EXPIRED" -> "Booking has expired.";
            case "BOOKING_CANNOT_CANCEL" -> "Booking cannot be cancelled.";
            case "BOOKING_CANNOT_MODIFY" -> "Booking cannot be modified.";
            case "BOOKING_DATE_OUT_OF_RANGE" -> "Booking date is outside the allowed booking window.";
            case "USER_PLATFORM_BANNED" -> "User is banned from booking.";
            case "USER_FIELD_BANNED" -> "User is banned from booking this field.";
            case "RECURRING_BOOKING_NOT_FOUND" -> "Recurring booking not found.";
            case "RECURRING_BOOKING_CONFLICT" -> "Recurring booking overlaps an existing recurring booking.";
            case "RECURRING_BOOKING_ALREADY_PAUSED" -> "Recurring booking is already paused.";
            case "RECURRING_BOOKING_ALREADY_ACTIVE" -> "Recurring booking is already active.";
            case "RECURRING_BOOKING_COMPLETED_BOOKING_REQUIRED" -> "A completed booking at this field is required before creating recurring bookings.";
            case "PAYMENT_NOT_FOUND" -> "Payment not found.";
            case "PAYMENT_DISPUTE_NOT_FOUND" -> "Payment dispute report not found.";
            case "PAYMENT_DISPUTE_ALREADY_REPORTED" -> "Payment dispute has already been reported.";
            case "PAYMENT_DISPUTE_ALREADY_REVIEWED" -> "Payment dispute report has already been reviewed.";
            case "NOTIFICATION_NOT_FOUND" -> "Notification not found.";
            case "USER_NOT_FOUND" -> "User not found.";
            case "BOOKING_CONFLICT" -> "Booking already exists.";
            case "RESERVATION_CONFLICT" -> "Reservation already exists.";
            case "SUBFIELD_CLOSED" -> "Sub-field is closed on the selected booking date.";
            case "RECURRING_SUBFIELD_CLOSED_ON_DATE" -> "A recurring booking occurrence falls on a sub-field closure date.";
            case "INSUFFICIENT_BALANCE" -> "Insufficient balance.";
            case "POST_NOT_FOUND" -> "Community post not found.";
            case "POST_ALREADY_REPORTED" -> "Community post has already been reported.";
            case "ALREADY_REPORTED" -> "Report already exists.";
            case "NO_SHOW_ALREADY_REPORTED" -> "No-show has already been reported.";
            case "UNAUTHORIZED" -> "Authentication is required.";
            case "FORBIDDEN" -> "Access is forbidden.";
            case "VALIDATION_ERROR" -> "Validation failed.";
            case "INVALID_REQUEST" -> "Invalid request.";
            case "CONFLICT" -> "Request conflict.";
            case "SERVICE_UNAVAILABLE" -> "Service unavailable.";
            case "INTERNAL_ERROR" -> "Internal server error.";
            default -> status.is5xxServerError() ? "Internal server error." : status.getReasonPhrase() + ".";
        };
    }

}
