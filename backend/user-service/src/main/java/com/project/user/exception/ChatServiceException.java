package com.project.user.exception;

import org.springframework.http.HttpStatus;

public class ChatServiceException extends RuntimeException {
    private final HttpStatus status;

    public ChatServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
