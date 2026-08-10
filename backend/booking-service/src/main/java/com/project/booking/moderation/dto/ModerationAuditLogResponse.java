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
public class ModerationAuditLogResponse {
    private UUID id;
    private UUID actorId;
    private UUID targetUserId;
    private String targetUsername;
    private String targetPhoneNumber;
    private UUID fieldId;
    private String action;
    private String details;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
