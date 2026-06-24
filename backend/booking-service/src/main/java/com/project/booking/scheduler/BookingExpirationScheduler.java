package com.project.booking.scheduler;

import com.project.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpirationScheduler {

    private final BookingService bookingService;

    @Scheduled(fixedDelayString = "${booking.expiration-scheduler-fixed-delay-ms:60000}")
    public void expirePendingBookings() {
        try {
            bookingService.expirePendingBookings();
        } catch (RuntimeException ex) {
            log.error("Failed to expire pending bookings", ex);
        }
    }
}
