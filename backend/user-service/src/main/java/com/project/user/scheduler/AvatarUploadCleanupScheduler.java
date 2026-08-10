package com.project.user.scheduler;

import com.project.common.scheduler.SchedulerJitter;
import com.project.user.service.AvatarUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AvatarUploadCleanupScheduler {
    private final AvatarUploadService service;

    @Value("${cloudinary.avatar-cleanup-jitter-ms:0}")
    private long schedulerJitterMs;

    @Scheduled(fixedDelayString = "${cloudinary.avatar-cleanup-interval-ms:3600000}")
    public void cleanup() {
        try {
            SchedulerJitter.sleepUpTo(schedulerJitterMs, "avatar-upload-cleanup");
            service.cleanupStaleUploads();
        } catch (RuntimeException e) {
            log.error("Avatar cleanup failed", e);
        }
    }
}
