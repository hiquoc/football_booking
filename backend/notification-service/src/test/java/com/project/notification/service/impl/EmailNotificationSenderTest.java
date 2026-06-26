package com.project.notification.service.impl;

import com.project.notification.dto.NotificationRequest;
import com.project.notification.enums.NotificationCode;
import com.project.notification.service.EmailTemplateService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotificationSenderTest {

    @Test
    void sendSkipsMissingRecipient() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailNotificationSender sender = new EmailNotificationSender(mailSender, mock(EmailTemplateService.class));

        sender.send(NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .code(NotificationCode.BOOKING_CONFIRMED)
                .title("Booking confirmed")
                .build());

        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    void sendRendersTemplateAndSendsMimeMessage() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailTemplateService templateService = mock(EmailTemplateService.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateService.render(org.mockito.ArgumentMatchers.any(NotificationRequest.class)))
                .thenReturn("<p>Confirmed</p>");
        EmailNotificationSender sender = new EmailNotificationSender(mailSender, templateService);

        sender.send(NotificationRequest.builder()
                .userId(UUID.randomUUID())
                .recipientEmail("user@example.com")
                .code(NotificationCode.BOOKING_CONFIRMED)
                .title("Booking confirmed")
                .payload(Map.of("bookingId", "123"))
                .build());

        verify(templateService).render(org.mockito.ArgumentMatchers.any(NotificationRequest.class));
        verify(mailSender).send(mimeMessage);
    }
}
