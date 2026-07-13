package com.project.user.dto;

import com.project.common.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private UserType userType;
    private String socialProvider;
    private String socialProviderId;
    private String status;
    private Long balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
