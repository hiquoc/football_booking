package com.project.user.dto;

import com.project.common.enums.UserType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SocialLoginRequest {
    @NotBlank(message = "Social login provider is required")
    private String provider; // GOOGLE or FACEBOOK

    @NotBlank(message = "Token is required")
    private String token;

    private String fullName;
    private UserType userType;
}
