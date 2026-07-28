package com.project.booking.community.dto;

import com.project.booking.community.enums.CommunityReportReason;
import com.project.booking.community.enums.CommunityReportStatus;
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
