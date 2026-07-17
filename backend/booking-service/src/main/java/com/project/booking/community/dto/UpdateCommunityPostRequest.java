package com.project.booking.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateCommunityPostRequest {
    @NotBlank
    @Size(max = 120)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotBlank
    @Size(max = 40)
    @Schema(example = "AVERAGE")
    private String skillLevel;

    @NotBlank
    @Pattern(regexp = "^[0-9+]{9,15}$")
    private String contactPhone;

    @Positive
    private Integer playersNeeded;
}
