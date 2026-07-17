package com.project.user.dto;

import com.project.user.enums.SkillLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileInfoDto {
    private UUID id;
    private String fullName;
    private String avatarUrl;
    private String phoneNumber;
    private String bio;
    private String teamPhotoUrl;
    private SkillLevel skillLevel;
}
