package com.project.field.scheduler;

import com.project.common.scheduler.SchedulerJitter;
import com.project.field.service.FieldImageUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImageUploadCleanupScheduler {
    private final FieldImageUploadService service;

    @Value("${cloudinary.placeholder-cleanup-jitter-ms:0}")
    private long schedulerJitterMs;

    @Scheduled(fixedDelayString = "${cloudinary.placeholder-cleanup-interval-ms:3600000}")
    public void cleanup() {
        try {
            SchedulerJitter.sleepUpTo(schedulerJitterMs, "image-upload-cleanup");
            service.cleanupStalePlaceholders();
        }
        catch (RuntimeException ex) { log.error("Failed to clean stale image upload placeholders", ex); }
    }
}
