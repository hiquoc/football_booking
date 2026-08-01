package com.project.booking.scheduler;

import com.project.booking.entity.RecurringBooking;
import com.project.booking.repository.RecurringBookingRepository;
import com.project.booking.service.RecurringBookingProcessor;
import com.project.common.enums.RecurringBookingStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecurringBookingSchedulerTest {

    @Test
    void processRecurringBookingsDelegatesToService() {
        UUID recurringId = UUID.randomUUID();
        RecurringBooking recurringBooking = RecurringBooking.builder().id(recurringId).build();
        RecurringBookingRepository recurringBookingRepository = mock(RecurringBookingRepository.class);
        RecurringBookingProcessor recurringBookingProcessor = mock(RecurringBookingProcessor.class);
        RecurringBookingScheduler scheduler = new RecurringBookingScheduler(
                recurringBookingProcessor,
                recurringBookingRepository);
        when(recurringBookingRepository.findByStatusAndNextProcessAtLessThanEqualOrderByNextProcessAtAsc(
                eq(RecurringBookingStatus.ACTIVE),
                any()))
                .thenReturn(List.of(recurringBooking));

        scheduler.processRecurringBookings();

        verify(recurringBookingProcessor).processOne(recurringId);
    }
}
