package com.project.user.exception;

import com.project.common.dto.ApiResponse;
import com.project.common.enums.ApiStatusCode;
import com.project.user.controller.ChatController;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ChatController.class)
public class ChatExceptionHandler {

    @ExceptionHandler(ChatServiceException.class)
    public ResponseEntity<ApiResponse<Void>> chatServiceException(ChatServiceException exception) {
        HttpStatus status = exception.status();
        ApiStatusCode statusCode = status == HttpStatus.UNAUTHORIZED ? ApiStatusCode.UNAUTHORIZED : ApiStatusCode.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(status).body(ApiResponse.error(statusCode, developerMessage(statusCode)));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> validation(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ApiStatusCode.VALIDATION_ERROR, "Validation failed."));
    }

    private String developerMessage(ApiStatusCode statusCode) {
        return statusCode == ApiStatusCode.UNAUTHORIZED ? "Authentication is required." : "Service unavailable.";
    }
}
