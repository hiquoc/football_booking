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
        super(message);
        this.status = status;
        this.code = code != null ? code : defaultCode(status);
        this.developerMessage = defaultDeveloperMessage(this.code, status);
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
            default -> status.is5xxServerError() ? "INTERNAL_SERVER_ERROR" : "UNKNOWN_ERROR";
        };
    }

    private static String defaultDeveloperMessage(String code, HttpStatus status) {
        return switch (code) {
            case "FIELD_NOT_FOUND" -> "Field not found.";
            case "SUBFIELD_NOT_FOUND" -> "Sub-field not found.";
            case "FIELD_TYPE_NOT_FOUND" -> "Field type not found.";
            case "BOOKING_NOT_FOUND" -> "Booking not found.";
            case "RECURRING_BOOKING_NOT_FOUND" -> "Recurring booking not found.";
            case "PAYMENT_NOT_FOUND" -> "Payment not found.";
            case "NOTIFICATION_NOT_FOUND" -> "Notification not found.";
            case "USER_NOT_FOUND" -> "User not found.";
            case "BOOKING_CONFLICT" -> "Booking already exists.";
            case "RESERVATION_CONFLICT" -> "Reservation already exists.";
            case "INSUFFICIENT_BALANCE" -> "Insufficient balance.";
            case "UNAUTHORIZED" -> "Authentication is required.";
            case "FORBIDDEN" -> "Access is forbidden.";
            case "VALIDATION_ERROR" -> "Validation failed.";
            case "INVALID_REQUEST" -> "Invalid request.";
            case "CONFLICT" -> "Request conflict.";
            case "SERVICE_UNAVAILABLE" -> "Service unavailable.";
            default -> status.is5xxServerError() ? "Unexpected server error." : status.getReasonPhrase() + ".";
        };
    }

}
