package com.project.notification.dto;

import com.project.notification.enums.NotificationCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification returned to the authenticated user.")
public class NotificationResponse {
    @Schema(description = "Notification ID.")
    private UUID id;
    @Schema(description = "Owner user ID.")
    private UUID userId;
    @Schema(description = "Stable code used by clients to choose presentation and navigation.")
    private NotificationCode code;
    @Schema(description = "Notification title.")
    private String title;
    @Schema(description = "Flexible event-specific payload.")
    private Map<String, Object> payload;
    @Schema(description = "Whether the authenticated user has read the notification.")
    private Boolean isRead;
    @Schema(description = "Creation timestamp.")
    private LocalDateTime createdAt;
    @Schema(description = "Read timestamp, if any.")
    private LocalDateTime readAt;
}
