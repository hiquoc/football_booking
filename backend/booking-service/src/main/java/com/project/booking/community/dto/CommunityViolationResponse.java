package com.project.booking.community.dto;

import com.project.booking.community.enums.CommunityModerationAction;
import com.project.booking.community.enums.CommunityViolationStatus;
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
public class CommunityViolationResponse {
    private UUID id;
    private UUID userId;
    private String reason;
    private CommunityModerationAction action;
    private LocalDateTime expireAt;
    private CommunityViolationStatus status;
    private UUID sourcePostId;
    private LocalDateTime createdAt;
}
