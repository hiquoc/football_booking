package com.project.field.scheduler;

import com.project.field.service.FieldImageUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImageUploadCleanupScheduler {
    private final FieldImageUploadService service;

    @Scheduled(fixedDelayString = "${cloudinary.placeholder-cleanup-interval-ms:3600000}")
    public void cleanup() {
        try { service.cleanupStalePlaceholders(); }
        catch (RuntimeException ex) { log.error("Failed to clean stale image upload placeholders", ex); }
    }
}
