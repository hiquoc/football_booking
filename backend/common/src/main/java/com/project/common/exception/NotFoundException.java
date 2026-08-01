package com.project.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessException {
    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public NotFoundException(String message, String code) {
        super(message, HttpStatus.NOT_FOUND, code);
    }
}
