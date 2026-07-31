package com.project.common.inbox.scheduler;

import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.service.InboxProcessingService;
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
public class InboxScheduler {

    private final InboxProcessingService processingService;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${inbox.poll-interval:1000}")
    public void processReceivedEvents() {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<InboxEvent> events = processingService.claimBatch();
            for (InboxEvent event : events) {
                process(event);
            }
        } finally {
            sample.stop(meterRegistry.timer("inbox.scheduler.duration"));
        }
    }

    private void process(InboxEvent event) {
        long startedAt = System.nanoTime();
        try {
            processingService.handle(event);
            processingService.markProcessed(event.getId());
            long processingTimeMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("kafka_consumer_processed eventId={} topic={} partition={} offset={} processingTimeMs={}",
                    event.getEventId(), event.getTopic(), event.getPartition(), event.getOffset(), processingTimeMs);
        } catch (Exception ex) {
            processingService.markFailed(event.getId(), ex.getMessage());
            log.error("kafka_consumer_processing_failed eventId={} topic={} partition={} offset={} retryCount={} reason={}",
                    event.getEventId(),
                    event.getTopic(),
                    event.getPartition(),
                    event.getOffset(),
                    event.getRetryCount() + 1,
                    ex.getMessage(),
                    ex);
        }
    }
}
