package com.project.booking.scheduler;

import com.project.booking.service.BookingService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BookingCompletionSchedulerTest {

    @Test
    void delegatesCompletionToBookingService() {
        BookingService bookingService = mock(BookingService.class);
        BookingCompletionScheduler scheduler = new BookingCompletionScheduler(bookingService);

        scheduler.completeFinishedBookings();

        verify(bookingService).completeFinishedBookings();
    }
}
