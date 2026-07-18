package com.project.booking.moderation.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class FieldViolationResponse {
    private UUID id;
    private UUID userId;
    private UUID fieldId;
    private Integer violationCount;
    private Boolean banned;
    private LocalDateTime banDate;
    private LocalDateTime lastViolationDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
