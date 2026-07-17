package com.project.user.dto;

import jakarta.validation.constraints.Size;
import com.project.user.enums.SkillLevel;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 1, max = 100, message = "Full name must be between 1 and 100 characters")
    private String fullName;

    @Size(max = 20, message = "Phone number must be at most 20 characters")
    private String phoneNumber;

    @Size(max = 500, message = "Bio must be at most 500 characters")
    private String bio;

    private SkillLevel skillLevel;
}
