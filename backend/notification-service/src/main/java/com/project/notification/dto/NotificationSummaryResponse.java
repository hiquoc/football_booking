package com.project.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Notification aggregate summary.")
public class NotificationSummaryResponse {
    @Schema(description = "Unread notification count.", example = "5")
    private long count;
}
