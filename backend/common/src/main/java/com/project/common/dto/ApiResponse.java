package com.project.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.project.common.enums.ApiStatusCode;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String statusCode;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return success(ApiStatusCode.SUCCESS, "Operation completed successfully", data);
    }

    public static <T> ApiResponse<T> success(ApiStatusCode statusCode, T data) {
        return success(statusCode, "Operation completed successfully", data);
    }

    public static <T> ApiResponse<T> success(ApiStatusCode statusCode, String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(statusCode.name())
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(ApiStatusCode.SUCCESS.name())
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return error(ApiStatusCode.INTERNAL_ERROR, message);
    }

    public static <T> ApiResponse<T> error(ApiStatusCode statusCode, String message) {
        return error(statusCode.name(), message);
    }

    public static <T> ApiResponse<T> error(String statusCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .statusCode(statusCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
