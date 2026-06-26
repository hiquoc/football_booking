package com.project.common.outbox.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "outbox")
public record OutboxProperties(
        int batchSize,
        long pollInterval,
        int maxRetries,
        int schedulerConcurrency,
        List<Duration> retryDelays,
        Dlq dlq
) {
    public OutboxProperties {
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
        if (dlq == null) {
            dlq = new Dlq(true, ".dlq");
        }
    }

    public record Dlq(boolean enabled, String topicSuffix) {
        public Dlq {
            if (topicSuffix == null || topicSuffix.isBlank()) {
                topicSuffix = ".dlq";
            }
        }
    }
}
