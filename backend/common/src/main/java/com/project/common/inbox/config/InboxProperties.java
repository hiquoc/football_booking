package com.project.common.inbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "inbox")
public record InboxProperties(
        int batchSize,
        long pollInterval,
        int maxRetries,
        int schedulerConcurrency,
        List<Duration> retryDelays
) {
    public InboxProperties {
        if (batchSize <= 0) {
            batchSize = 50;
        }
        if (pollInterval <= 0) {
            pollInterval = 5000;
        }
        if (maxRetries <= 0) {
            maxRetries = 5;
        }
        if (schedulerConcurrency <= 0) {
            schedulerConcurrency = 1;
        }
        if (retryDelays == null || retryDelays.isEmpty()) {
            retryDelays = List.of(
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(30),
                    Duration.ofMinutes(1),
                    Duration.ofMinutes(5),
                    Duration.ofMinutes(15));
        }
    }
}
