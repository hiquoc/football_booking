package com.project.booking.scheduler;

import com.project.booking.service.RecurringBookingService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RecurringBookingSchedulerTest {

    @Test
    void processRecurringBookingsDelegatesToService() {
        RecurringBookingService recurringBookingService = mock(RecurringBookingService.class);
        RecurringBookingScheduler scheduler = new RecurringBookingScheduler(recurringBookingService);

        scheduler.processRecurringBookings();

        verify(recurringBookingService).processDue(any());
    }
}
