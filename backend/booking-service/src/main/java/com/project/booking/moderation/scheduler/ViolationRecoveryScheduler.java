package com.project.booking.moderation.scheduler;

import com.project.booking.moderation.service.BookingModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViolationRecoveryScheduler {
    private final BookingModerationService service;

    @Scheduled(cron = "${moderation.violation-recovery-cron:0 0 3 1 * *}")
    public void recoverViolations() {
        int changed = service.recoverMonthlyViolations();
        if (changed > 0) {
            log.info("Recovered {} field violation records", changed);
        }
    }
}
