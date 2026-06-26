package com.project.common.inbox.service;

import com.project.common.inbox.entity.InboxEventStatus;
import com.project.common.inbox.repository.InboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InboxMetrics {

    public InboxMetrics(MeterRegistry meterRegistry, InboxEventRepository repository) {
        for (InboxEventStatus status : InboxEventStatus.values()) {
            meterRegistry.gauge("inbox.events", List.of(Tag.of("status", status.name())),
                    repository, repo -> repo.countByStatus(status));
        }
    }
}
