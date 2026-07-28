package com.project.common.dto.balance;

public record BalanceDeductionResponse(
        boolean deducted,
        long balance,
        String message
) {
}
