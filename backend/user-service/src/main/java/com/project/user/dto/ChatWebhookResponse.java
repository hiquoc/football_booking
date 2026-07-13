package com.project.user.dto;

import org.springframework.http.MediaType;

public record ChatWebhookResponse(
        int status,
        String body,
        MediaType contentType
) {
}
