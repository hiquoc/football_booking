package com.project.booking.moderation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Field-level client violation details, including manager-visible client contact data on owner moderation endpoints")
public class FieldViolationResponse {
    private UUID id;
    private UUID userId;
    @Schema(description = "Client display name from the booking service user projection")
    private String username;
    @Schema(description = "Unmasked client phone number for authorized field managers")
    private String phoneNumber;
    private UUID fieldId;
    private Integer violationCount;
    private Boolean banned;
    private LocalDateTime banDate;
    private LocalDateTime lastViolationDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
