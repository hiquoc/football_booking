package com.project.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChatClientRequest(
        @NotBlank(message = "Message is required")
        @Size(max = 4000, message = "Message must be 4000 characters or fewer")
        String message,
        UUID conversationId
) {
}
