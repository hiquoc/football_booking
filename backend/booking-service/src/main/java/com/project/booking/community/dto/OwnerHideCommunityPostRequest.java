package com.project.booking.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OwnerHideCommunityPostRequest {
    @NotBlank
    @Size(max = 500)
    private String reason;
}
