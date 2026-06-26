package com.project.notification.mapper;

import com.project.notification.dto.NotificationResponse;
import com.project.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification entity) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .code(entity.getCode())
                .title(entity.getTitle())
                .payload(entity.getPayload())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .readAt(entity.getReadAt())
                .build();
    }
}
