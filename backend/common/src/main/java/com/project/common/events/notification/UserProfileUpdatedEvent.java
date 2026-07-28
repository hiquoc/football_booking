package com.project.common.events.notification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UserProfileUpdatedEvent(
        UUID userId,
        String email,
        String fullName,
        String phoneNumber,
        String avatarUrl,
        String bio,
        String teamPhotoUrl,
        String skillLevel,
        String userType,
        String status,
        Long balance,
        Integer totalMatches,
        Integer wins,
        Integer draws,
        Integer losses,
        BigDecimal noCancelRate,
        BigDecimal onTimeRate,
        BigDecimal fairPlayRate,
        Integer completedBookingCount,
        Instant occurredAt
) {
}
