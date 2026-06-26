package com.project.notification.service.impl;

import com.project.notification.dto.NotificationRequest;
import com.project.notification.enums.NotificationChannel;
import com.project.notification.service.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsNotificationSender implements NotificationSender {
    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(NotificationRequest request) {
        log.debug("SMS notification channel is not implemented yet: userId={}, code={}",
                request.getUserId(), request.getCode());
    }
}
