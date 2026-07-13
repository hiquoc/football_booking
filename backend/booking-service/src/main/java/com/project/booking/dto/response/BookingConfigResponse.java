package com.project.booking.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookingConfigResponse(
        UUID id,
        Long bookingFee,
        Integer refundBeforeHours,
        Boolean refundEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
