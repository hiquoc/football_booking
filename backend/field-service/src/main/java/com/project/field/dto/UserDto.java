package com.project.field.dto;

import com.project.common.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

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
    private String status;
}
