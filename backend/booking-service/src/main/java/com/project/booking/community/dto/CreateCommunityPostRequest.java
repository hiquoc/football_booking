package com.project.booking.community.dto;

import com.project.booking.community.enums.CommunityPostType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateCommunityPostRequest {
    @NotNull
    private UUID bookingId;

    @NotNull
    private CommunityPostType postType;

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

    @Size(max = 255)
    private String ownerDisplayName;

    @Size(max = 1000)
    private String ownerAvatarUrl;

    @Size(max = 1000)
    private String ownerTeamPhotoUrl;
}
