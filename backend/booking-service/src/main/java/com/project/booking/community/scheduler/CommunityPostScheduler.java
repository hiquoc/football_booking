package com.project.booking.community.scheduler;

import com.project.booking.community.service.CommunityPostMaintenanceService;
import com.project.common.scheduler.SchedulerJitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityPostScheduler {
    private final CommunityPostMaintenanceService maintenanceService;

    @Value("${community.post-close-scheduler-jitter-ms:0}")
    private long schedulerJitterMs;

    @Scheduled(fixedDelayString = "${community.post-close-scheduler-fixed-delay-ms:1800000}")
    public void closeStartedPosts() {
        SchedulerJitter.sleepUpTo(schedulerJitterMs, "community-post-close");
        int closed = maintenanceService.closeEndedActivePosts();
        if (closed > 0) {
            log.info("Closed {} community posts whose match already ended", closed);
        }
    }
}
