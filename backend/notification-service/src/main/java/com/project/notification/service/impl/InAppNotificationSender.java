package com.project.notification.service.impl;

import com.project.notification.dto.NotificationRequest;
import com.project.notification.enums.NotificationChannel;
import com.project.notification.service.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InAppNotificationSender implements NotificationSender {
    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public void send(NotificationRequest request) {
        log.debug("IN_APP notification persisted for userId={}, code={}", request.getUserId(), request.getCode());
    }
}
