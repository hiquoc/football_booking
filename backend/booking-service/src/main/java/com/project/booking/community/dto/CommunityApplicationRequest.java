package com.project.booking.community.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommunityApplicationRequest {
    @Size(max = 1000)
    private String message;

    @Size(max = 255)
    private String applicantDisplayName;

    @Size(max = 1000)
    private String applicantAvatarUrl;

    @Size(max = 1000)
    private String applicantTeamPhotoUrl;

    @Size(max = 40)
    private String applicantSkillLevel;
}
