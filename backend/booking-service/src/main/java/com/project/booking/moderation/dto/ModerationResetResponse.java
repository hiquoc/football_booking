package com.project.booking.moderation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationResetResponse {
    private UUID userId;
    private int platformBanRecordsCleared;
    private int fieldViolationRecordsReset;
}
