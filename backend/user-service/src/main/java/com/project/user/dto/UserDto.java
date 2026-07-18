package com.project.user.dto;

import com.project.common.enums.UserType;
import com.project.user.enums.SkillLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String phoneNumber;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String bio;
    private String teamPhotoUrl;
    private SkillLevel skillLevel;
    private Integer totalMatches;
    private Integer wins;
    private Integer draws;
    private Integer losses;
    private BigDecimal noCancelRate;
    private BigDecimal onTimeRate;
    private BigDecimal fairPlayRate;
    private UserType userType;
    private String socialProvider;
    private String socialProviderId;
    private String status;
    private Long balance;
    private Integer completedBookingCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
