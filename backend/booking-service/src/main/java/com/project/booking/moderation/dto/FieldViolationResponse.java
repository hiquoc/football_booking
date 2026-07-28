package com.project.booking.moderation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
