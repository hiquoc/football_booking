package com.project.booking.scheduler;

import com.project.booking.service.RecurringBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringBookingScheduler {

    private final RecurringBookingService recurringBookingService;

    @Scheduled(fixedDelayString = "${booking.recurring-scheduler-fixed-delay-ms:43200000}")
    public void processRecurringBookings() {
        try {
            recurringBookingService.processDue(LocalDateTime.now());
        } catch (RuntimeException ex) {
            log.error("Failed to process recurring bookings", ex);
        }
    }
}
