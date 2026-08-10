package com.project.common.exception;

import com.project.common.dto.ApiResponse;
import com.project.common.enums.ApiStatusCode;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.warn("request_rejected reason=access_denied path={} message={}",
                path(request), ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error(ApiStatusCode.FORBIDDEN, "Access is forbidden."), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex, WebRequest request) {
        if (ex.getStatus().is4xxClientError()) {
            log.warn("business_request_rejected code={} status={} path={} message={}",
                    ex.getCode(), ex.getStatus().value(), path(request), ex.getMessage());
        }
        return new ResponseEntity<>(ApiResponse.error(ex.getCode(), ex.getDeveloperMessage()), ex.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex,WebRequest request) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String detailMessage = "Validation failed.";
        log.warn("validation_failed statusCode={} path={} field={} message={}",
                "VALIDATION_ERROR",
                path(request),
                fieldError != null ? fieldError.getField() : null,
                fieldError != null ? fieldError.getDefaultMessage() : null);

        return ResponseEntity.badRequest().body(ApiResponse.error(ApiStatusCode.VALIDATION_ERROR, detailMessage));
    }

    @ExceptionHandler({ConstraintViolationException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(Exception ex, WebRequest request) {
        log.warn("invalid_request statusCode={} path={} exceptionClass={} message={}",
                "VALIDATION_ERROR",
                path(request),
                ex.getClass().getName(),
                ex.getMessage());

        return ResponseEntity.badRequest().body(ApiResponse.error(ApiStatusCode.VALIDATION_ERROR, "Validation failed."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllExceptions(Exception ex, WebRequest request) {
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
        return new ResponseEntity<>(ApiResponse.error(ApiStatusCode.INTERNAL_ERROR, "Internal server error."), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String path(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        String description = request.getDescription(false);
        return description != null && description.startsWith("uri=") ? description.substring(4) : description;
    }
}
