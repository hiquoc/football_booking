package com.project.booking.community.dto;

import com.project.booking.community.enums.CommunityApplicationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CommunityApplicationResponse {
    private UUID id;
    private UUID postId;
    private UUID applicantId;
    private CommunityApplicationStatus status;
    private String message;
    private String applicantDisplayName;
    private String applicantAvatarUrl;
    private String applicantTeamPhotoUrl;
    private String applicantSkillLevel;
    private LocalDateTime decidedAt;
    private LocalDateTime withdrawnAt;
    private LocalDateTime createdAt;
}
