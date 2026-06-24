package com.project.common.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AuditDto {
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
