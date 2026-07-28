package com.project.common.dto.balance;

import java.util.UUID;

public record BalanceDeductionRequest(
        UUID userId,
        long amount,
        UUID bookingId,
        String bookingCode,
        String reason
) {
}
