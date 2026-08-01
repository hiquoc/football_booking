package com.project.common.exception;

import com.project.common.dto.ErrorResponse;
import com.project.common.logging.MdcFields;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("request_rejected reason=access_denied path={} message={}",
                path(request), ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("FORBIDDEN")
                .statusCode("FORBIDDEN")
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message("Access is forbidden.")
                .path(path(request))
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, WebRequest request) {
        if (ex.getStatus().is4xxClientError()) {
            log.warn("business_request_rejected code={} status={} path={} message={}",
                    ex.getCode(), ex.getStatus().value(), path(request), ex.getMessage());
        }
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ex.getCode())
                .statusCode(ex.getCode())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getDeveloperMessage())
                .path(path(request))
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex,WebRequest request) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String detailMessage = "Validation failed.";
        log.warn("validation_failed statusCode={} path={} field={} message={}",
                "VALIDATION_ERROR",
                path(request),
                fieldError != null ? fieldError.getField() : null,
                fieldError != null ? fieldError.getDefaultMessage() : null);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .statusCode("VALIDATION_ERROR")
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(detailMessage)
                .path(path(request))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler({ConstraintViolationException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception ex, WebRequest request) {
        log.warn("invalid_request statusCode={} path={} exceptionClass={} message={}",
                "VALIDATION_ERROR",
                path(request),
                ex.getClass().getName(),
                ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .statusCode("VALIDATION_ERROR")
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed.")
                .path(path(request))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {
        HttpServletRequest servletRequest = request instanceof ServletWebRequest servletWebRequest
                ? servletWebRequest.getRequest()
                : null;
        String path = servletRequest != null ? servletRequest.getRequestURI() : request.getDescription(false);
        String method = servletRequest != null ? servletRequest.getMethod() : "unknown";
        log.error("unhandled_request_error requestId={} userId={} method={} path={} exceptionClass={} message={}",
                MDC.get(MdcFields.REQUEST_ID),
                MDC.get(MdcFields.USER_ID),
                method,
                path,
                ex.getClass().getName(),
                ex.getMessage(),
                ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("INTERNAL_SERVER_ERROR")
                .statusCode("INTERNAL_SERVER_ERROR")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Unexpected server error.")
                .path(path(request))
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String path(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        String description = request.getDescription(false);
        return description != null && description.startsWith("uri=") ? description.substring(4) : description;
    }
}
