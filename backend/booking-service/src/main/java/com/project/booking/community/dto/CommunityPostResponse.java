package com.project.booking.community.dto;

import com.project.booking.community.enums.CommunityPostStatus;
import com.project.booking.community.enums.CommunityPostType;
import com.project.common.enums.SubFieldType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPostResponse {
    private UUID id;
    private UUID bookingId;
    private UUID ownerId;
    private CommunityPostType postType;
    private CommunityPostStatus status;
    private String title;
    private String description;
    private String skillLevel;
    private String contactPhone;
    private Integer playersNeeded;
    private Integer acceptedPlayersCount;
    private String bookingCode;
    private UUID fieldId;
    private UUID fieldOwnerId;
    private String fieldName;
    private UUID subFieldId;
    private String subFieldName;
    private SubFieldType fieldType;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String ownerDisplayName;
    private String ownerAvatarUrl;
    private String ownerTeamPhotoUrl;
    private String locationText;
    private UUID matchedApplicationId;
    private LocalDateTime closedAt;
    private LocalDateTime hiddenAt;
    private String hiddenReason;
    private Boolean ownerUnderModeration;
    private CommunityPlayerStatisticsResponse ownerStatistics;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommunityApplicationResponse> applications;
}
