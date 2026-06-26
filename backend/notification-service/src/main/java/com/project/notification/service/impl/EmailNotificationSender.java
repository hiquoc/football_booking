package com.project.notification.service.impl;

import com.project.notification.dto.NotificationRequest;
import com.project.notification.enums.NotificationChannel;
import com.project.notification.service.EmailTemplateService;
import com.project.notification.service.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final EmailTemplateService emailTemplateService;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(NotificationRequest request) {
        if (!StringUtils.hasText(request.getRecipientEmail())) {
            log.info("Skipping EMAIL notification without recipient: userId={}, code={}",
                    request.getUserId(), request.getCode());
            return;
        }
        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(request.getRecipientEmail());
            helper.setSubject(request.getTitle());
            helper.setText(emailTemplateService.render(request), true);
            mailSender.send(message);
            log.info("Sent EMAIL notification: userId={}, code={}", request.getUserId(), request.getCode());
        } catch (Exception ex) {
            log.error("Failed to send EMAIL notification: userId={}, code={}", request.getUserId(), request.getCode(), ex);
        }
    }
}
