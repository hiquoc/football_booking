package com.project.booking.community.scheduler;

import com.project.booking.community.service.CommunityPostMaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityPostScheduler {
    private final CommunityPostMaintenanceService maintenanceService;

    @Scheduled(fixedDelayString = "${community.post-close-scheduler-fixed-delay-ms:60000}")
    public void closeStartedPosts() {
        int closed = maintenanceService.closeStartedOpenPosts();
        if (closed > 0) {
            log.info("Closed {} community posts whose match already started", closed);
        }
    }
}
