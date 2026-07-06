package com.project.booking.scheduler;

import com.project.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCompletionScheduler {

    private final BookingService bookingService;

    @Scheduled(fixedDelayString = "${booking.completion-scheduler-fixed-delay-ms:300000}")
    public void completeFinishedBookings() {
        try {
            bookingService.completeFinishedBookings();
        } catch (RuntimeException ex) {
            log.error("Failed to complete finished bookings", ex);
        }
    }
}
