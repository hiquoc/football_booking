package com.project.user.dto;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record AvatarUploadSlotRequest(@NotNull UUID requestId) {}
