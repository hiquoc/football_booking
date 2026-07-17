package com.project.booking.community.dto;

import com.project.booking.community.enums.CommunityReportReason;
import com.project.booking.community.enums.CommunityReportStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CommunityReportResponse {
    private UUID id;
    private UUID postId;
    private UUID reporterId;
    private CommunityReportReason reason;
    private String description;
    private CommunityReportStatus status;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private CommunityPostResponse post;
}
