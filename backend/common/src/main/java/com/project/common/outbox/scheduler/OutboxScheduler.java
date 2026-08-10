package com.project.common.outbox.scheduler;

import com.project.common.scheduler.SchedulerJitter;
import com.project.common.outbox.entity.OutboxEvent;
import com.project.common.outbox.service.OutboxProcessingService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class OutboxScheduler {

    private final OutboxProcessingService processingService;
    private final MeterRegistry meterRegistry;
    private final ExecutorService outboxPublisherExecutor;
    private final long schedulerJitterMs;

    public OutboxScheduler(
            OutboxProcessingService processingService,
            MeterRegistry meterRegistry,
            @Value("${outbox.scheduler-jitter-ms:0}") long schedulerJitterMs,
            @Qualifier("outboxPublisherExecutor") ExecutorService outboxPublisherExecutor) {
        this.processingService = processingService;
        this.meterRegistry = meterRegistry;
        this.schedulerJitterMs = schedulerJitterMs;
        this.outboxPublisherExecutor = outboxPublisherExecutor;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval:1000}")
    public void publishPendingEvents() {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            SchedulerJitter.sleepUpTo(schedulerJitterMs, "outbox");
            List<OutboxEvent> events = processingService.claimBatch();
            if (events.isEmpty()) {
                return;
            }
            CompletableFuture<?>[] futures = events.stream()
                    .map(event -> CompletableFuture.runAsync(() -> publish(event), outboxPublisherExecutor)
                            .exceptionally(ex -> {
                                log.error("Unexpected failure while processing outbox event: eventId={}", event.getId(), ex);
                                return null;
                            }))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(futures).join();
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
