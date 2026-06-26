package com.project.notification.service;

import com.project.notification.dto.NotificationRequest;
import com.project.notification.enums.NotificationChannel;

public interface NotificationSender {
    NotificationChannel getChannel();

    void send(NotificationRequest request);
}
