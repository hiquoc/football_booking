package com.project.notification.service;

import com.project.notification.dto.NotificationRequest;

public interface EmailTemplateService {
    String render(NotificationRequest request);
}
