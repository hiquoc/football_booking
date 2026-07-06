package com.project.user.scheduler;
import com.project.user.service.AvatarUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor @Slf4j
public class AvatarUploadCleanupScheduler {
    private final AvatarUploadService service;
    @Scheduled(fixedDelayString="${cloudinary.avatar-cleanup-interval-ms:3600000}")
    public void cleanup() { try { service.cleanupStaleUploads(); } catch (RuntimeException e) { log.error("Avatar cleanup failed", e); } }
}
