package com.project.booking.community.scheduler;

import com.project.booking.community.service.CommunityModerationService;
import com.project.common.scheduler.SchedulerJitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityModerationScheduler {
    private final CommunityModerationService moderationService;

    @Value("${community.moderation.expiration-scheduler-jitter-ms:0}")
    private long schedulerJitterMs;

    @Scheduled(fixedDelayString = "${community.moderation.expiration-scheduler-fixed-delay-ms:300000}")
    public void expireViolations() {
        SchedulerJitter.sleepUpTo(schedulerJitterMs, "community-moderation-expiration");
        int expired = moderationService.expireViolations();
        if (expired > 0) {
            log.info("Expired {} community moderation violations", expired);
        }
    }
}
