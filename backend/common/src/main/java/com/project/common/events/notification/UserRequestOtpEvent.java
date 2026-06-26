package com.project.common.events.notification;

import java.time.Instant;

public record UserRequestOtpEvent(
        String phoneNumber,
        Instant requestedAt
) {
}
