package com.project.field.exceptions;

import com.project.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ImageStorageException extends BusinessException {

    private static final String ERROR_CODE = "IMAGE_STORAGE_ERROR";

    public ImageStorageException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_GATEWAY, ERROR_CODE);
        initCause(cause);
    }
}
