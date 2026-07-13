package com.project.user.exception;

import com.project.user.controller.ChatController;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice(assignableTypes = ChatController.class)
public class ChatExceptionHandler {

    @ExceptionHandler(ChatServiceException.class)
    public ResponseEntity<Map<String, Object>> chatServiceException(ChatServiceException exception) {
        return ResponseEntity.status(exception.status()).body(error(exception.status(), exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<Map<String, Object>> validation(Exception exception) {
        String message = exception instanceof MethodArgumentNotValidException bindException
                ? bindException.getFieldErrors().stream().findFirst()
                        .map(fieldError -> fieldError.getDefaultMessage())
                        .orElse("Invalid request")
                : "Invalid request";
        return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST, message));
    }

    private Map<String, Object> error(HttpStatus status, String message) {
        return Map.of(
                "code", status.name(),
                "status", status.value(),
                "message", message,
                "timestamp", OffsetDateTime.now().toString()
        );
    }
}
