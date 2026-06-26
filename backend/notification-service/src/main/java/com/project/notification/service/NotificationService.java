package com.project.notification.service;

import com.project.common.dto.PageResponse;
import com.project.notification.dto.NotificationRequest;
import com.project.notification.dto.NotificationResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    NotificationResponse create(NotificationRequest request);

    PageResponse<NotificationResponse> getNotifications(UUID userId, Pageable pageable);

    List<NotificationResponse> getUnreadNotifications(UUID userId);

    NotificationResponse markAsRead(UUID userId, UUID notificationId);

    void markAllAsRead(UUID userId);

    long countUnread(UUID userId);
}
