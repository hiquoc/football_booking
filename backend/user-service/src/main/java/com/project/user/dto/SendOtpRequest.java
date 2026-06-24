package com.project.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendOtpRequest {
    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^(0|\\+84)[0-9]{9}$",
            message = "Invalid phone number"
    )
    private String phoneNumber;
}
