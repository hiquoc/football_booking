package com.project.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    /**
     * Deprecated compatibility alias. New clients should use statusCode.
     */
    private String code;
    private String statusCode;
    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
}
