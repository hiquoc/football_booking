package com.project.booking.scheduler;

import com.project.booking.repository.RecurringBookingRepository;
import com.project.booking.service.RecurringBookingProcessor;
import com.project.common.enums.RecurringBookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringBookingScheduler {

    private final RecurringBookingProcessor recurringBookingProcessor;
    private final RecurringBookingRepository recurringBookingRepository;

    @Scheduled(fixedDelayString = "${booking.recurring-scheduler-fixed-delay-ms:43200000}")
    public void processRecurringBookings() {
        try {
            LocalDateTime now = LocalDateTime.now();
            recurringBookingRepository
                    .findByStatusAndNextProcessAtLessThanEqualOrderByNextProcessAtAsc(RecurringBookingStatus.ACTIVE, now)
                    .forEach(recurringBooking -> {
                        try {
                            recurringBookingProcessor.processOne(recurringBooking.getId());
                        } catch (RuntimeException ex) {
                            log.error("Failed to process recurring booking id={}", recurringBooking.getId(), ex);
                        }
                    });
        } catch (RuntimeException ex) {
            log.error("Failed to process recurring bookings", ex);
        }
    }
}
