package com.project.notification.dto;

import com.project.notification.enums.NotificationChannel;
import com.project.notification.enums.NotificationCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Internal notification creation request built from domain events.")
public class NotificationRequest {
    @Schema(description = "Recipient user ID.")
    private UUID userId;
    @Schema(description = "Email recipient for EMAIL channel.")
    private String recipientEmail;
    @Schema(description = "Stable notification code used by clients for presentation and navigation.")
    private NotificationCode code;
    @Schema(description = "Human-readable notification title.")
    private String title;
    @Schema(description = "Flexible event-specific payload stored as JSON.")
    private Map<String, Object> payload;
    @Schema(description = "Channels used to deliver the notification.")
    private List<NotificationChannel> channels;
}
