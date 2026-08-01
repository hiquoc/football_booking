package com.project.booking.service;

import com.project.booking.entity.RecurringBooking;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.BookingSubFieldProjectionRepository;
import com.project.booking.repository.RecurringBookingRepository;
import com.project.common.enums.RecurringBookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringBookingProcessorTest {

    @Mock
    private RecurringBookingRepository recurringBookingRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSubFieldProjectionRepository subFieldRepository;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private RecurringBookingProcessor recurringBookingProcessor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recurringBookingProcessor, "generationLeadDays", 0);
    }

    @Test
    void processOneAdvancesNextProcessAtByIntervalDays() {
        UUID recurringId = UUID.randomUUID();
        LocalDateTime nextProcessAt = LocalDateTime.now().minusMinutes(1);
        RecurringBooking recurringBooking = RecurringBooking.builder()
                .id(recurringId)
                .userId(UUID.randomUUID())
                .fieldId(UUID.randomUUID())
                .subFieldId(UUID.randomUUID())
                .startDate(nextProcessAt.toLocalDate())
                .endDate(nextProcessAt.toLocalDate().plusDays(10))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .intervalDays(3)
                .status(RecurringBookingStatus.ACTIVE)
                .nextProcessAt(nextProcessAt)
                .build();
        when(recurringBookingRepository.findById(recurringId)).thenReturn(Optional.of(recurringBooking));
        when(bookingRepository.existsBySourceRecurringBookingIdAndBookingDate(eq(recurringId), any())).thenReturn(false);

        recurringBookingProcessor.processOne(recurringId);

        assertEquals(nextProcessAt.toLocalDate().plusDays(3).atStartOfDay(), recurringBooking.getNextProcessAt());
        verify(bookingService).createRecurringOccurrence(eq(recurringBooking.getUserId()), eq(recurringId), any());
        verify(recurringBookingRepository).save(recurringBooking);
    }

    @Test
    void processOneCompletesRecurringBookingAfterLastMatchIsBooked() {
        UUID recurringId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDateTime nextProcessAt = LocalDateTime.now().minusMinutes(1);
        RecurringBooking recurringBooking = RecurringBooking.builder()
                .id(recurringId)
                .userId(UUID.randomUUID())
                .fieldId(UUID.randomUUID())
                .subFieldId(subFieldId)
                .startDate(nextProcessAt.toLocalDate().minusDays(6))
                .endDate(nextProcessAt.toLocalDate())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .intervalDays(3)
                .status(RecurringBookingStatus.ACTIVE)
                .nextProcessAt(nextProcessAt)
                .build();
        when(recurringBookingRepository.findById(recurringId)).thenReturn(Optional.of(recurringBooking));
        when(bookingRepository.existsBySourceRecurringBookingIdAndBookingDate(eq(recurringId), any())).thenReturn(false);
        when(recurringBookingRepository.existsBySubFieldIdAndStatus(subFieldId, RecurringBookingStatus.ACTIVE))
                .thenReturn(false);

        recurringBookingProcessor.processOne(recurringId);

        assertEquals(RecurringBookingStatus.COMPLETED, recurringBooking.getStatus());
        assertNull(recurringBooking.getNextProcessAt());
        verify(bookingService).createRecurringOccurrence(eq(recurringBooking.getUserId()), eq(recurringId), any());
        verify(subFieldRepository).updateHasRecurring(subFieldId, false);
        verify(recurringBookingRepository).save(recurringBooking);
    }
}
