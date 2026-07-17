package com.project.booking.community.scheduler;

import com.project.booking.community.service.CommunityModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityModerationScheduler {
    private final CommunityModerationService moderationService;

    @Scheduled(fixedDelayString = "${community.moderation.expiration-scheduler-fixed-delay-ms:300000}")
    public void expireViolations() {
        int expired = moderationService.expireViolations();
        if (expired > 0) {
            log.info("Expired {} community moderation violations", expired);
        }
    }
}
