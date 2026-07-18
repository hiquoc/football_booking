package com.project.user.repository;

public record UserReputationSummary(
        long total,
        long onTimeCount,
        long noCancelCount,
        long fairPlayCount
) {
}
