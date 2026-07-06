package com.project.user.dto;
import jakarta.validation.constraints.*;
public record AvatarUploadConfirmRequest(@NotBlank String publicId, @NotBlank @Size(max=500) String secureUrl,
        @Positive long version, @NotBlank String signature,
        @NotBlank @Pattern(regexp="(?i)jpg|jpeg|png|webp") String format,
        @Positive int width, @Positive int height, @Positive long bytes) {}
