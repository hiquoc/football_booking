package com.project.common.outbox.scheduler;

import com.project.common.outbox.entity.OutboxEvent;
import com.project.common.outbox.service.OutboxProcessingService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

    private final OutboxProcessingService processingService;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${outbox.poll-interval:5000}")
    public void publishPendingEvents() {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<OutboxEvent> events = processingService.claimBatch();
            for (OutboxEvent event : events) {
                publish(event);
            }
        } finally {
            sample.stop(meterRegistry.timer("outbox.scheduler.duration"));
        }
    }

    private void publish(OutboxEvent event) {
        try {
            processingService.publish(event);
            processingService.markPublished(event.getId());
        } catch (Exception ex) {
            String message = ex.getMessage();
            boolean maxRetriesReached = processingService.markFailed(event.getId(), message);
            if (maxRetriesReached) {
                try {
                    processingService.publishDlq(event, message);
                    processingService.markDeadLetter(event.getId(), message);
                } catch (Exception dlqEx) {
                    log.error("Failed to publish outbox event or DLQ: eventId={}", event.getId(), dlqEx);
                }
            }
        }
    }
}
