package com.project.notification.service.impl;

import com.project.notification.dto.NotificationRequest;
import com.project.notification.enums.NotificationCode;
import com.project.notification.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailTemplateServiceImpl implements EmailTemplateService {

    private static final Map<NotificationCode, String> TEMPLATES = new EnumMap<>(NotificationCode.class);

    static {
        TEMPLATES.put(NotificationCode.BOOKING_CONFIRMED, "email/booking-confirmed");
        TEMPLATES.put(NotificationCode.BOOKING_CANCELLED, "email/booking-cancelled");
        TEMPLATES.put(NotificationCode.PAYMENT_SUCCESS, "email/payment-success");
        TEMPLATES.put(NotificationCode.BOOKING_CREATED, "email/welcome");
    }

    private final SpringTemplateEngine templateEngine;

    @Override
    public String render(NotificationRequest request) {
        String template = TEMPLATES.getOrDefault(request.getCode(), "email/welcome");
        Context context = new Context();
        context.setVariable("title", request.getTitle());
        context.setVariable("payload", request.getPayload());
        return templateEngine.process(template, context);
    }
}
