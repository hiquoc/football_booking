package com.project.booking.community.dto;

import com.project.booking.community.enums.CommunityModerationAction;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CommunityModerationHistoryResponse {
    private UUID id;
    private UUID targetUserId;
    private UUID targetPostId;
    private UUID moderatorId;
    private CommunityModerationAction action;
    private String reason;
    private String note;
    private LocalDateTime createdAt;
}
