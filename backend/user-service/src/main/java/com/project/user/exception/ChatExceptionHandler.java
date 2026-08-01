package com.project.user.exception;

import com.project.common.dto.ErrorResponse;
import com.project.user.controller.ChatController;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice(assignableTypes = ChatController.class)
public class ChatExceptionHandler {

    @ExceptionHandler(ChatServiceException.class)
    public ResponseEntity<ErrorResponse> chatServiceException(ChatServiceException exception) {
        HttpStatus status = exception.status();
        String statusCode = status == HttpStatus.UNAUTHORIZED ? "UNAUTHORIZED" : "SERVICE_UNAVAILABLE";
        return ResponseEntity.status(status).body(error(status, statusCode, developerMessage(statusCode)));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> validation(Exception exception) {
        return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed."));
    }

    private ErrorResponse error(HttpStatus status, String statusCode, String message) {
        return ErrorResponse.builder()
                .code(statusCode)
                .statusCode(statusCode)
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path("/api/v1/chat")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String developerMessage(String statusCode) {
        return "UNAUTHORIZED".equals(statusCode) ? "Authentication is required." : "Service unavailable.";
    }
}
