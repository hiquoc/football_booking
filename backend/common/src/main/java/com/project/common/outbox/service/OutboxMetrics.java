package com.project.common.outbox.service;

import com.project.common.outbox.entity.OutboxEventStatus;
import com.project.common.outbox.repository.OutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OutboxMetrics {

    public OutboxMetrics(MeterRegistry meterRegistry, OutboxEventRepository repository) {
        for (OutboxEventStatus status : OutboxEventStatus.values()) {
            meterRegistry.gauge("outbox.events", java.util.List.of(io.micrometer.core.instrument.Tag.of("status", status.name())),
                    repository, repo -> repo.countByStatus(status));
        }
    }
}
