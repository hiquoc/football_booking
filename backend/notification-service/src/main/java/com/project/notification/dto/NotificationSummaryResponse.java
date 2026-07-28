package com.project.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification aggregate summary.")
public class NotificationSummaryResponse {
    @Schema(description = "Unread notification count.", example = "5")
    private long count;
}
