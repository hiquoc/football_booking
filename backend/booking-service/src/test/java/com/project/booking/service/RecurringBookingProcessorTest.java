package com.project.booking.service;

import com.project.booking.entity.RecurringBooking;
import com.project.booking.kafka.RecurringBookingOccurrenceEventPublisher;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.BookingSubFieldProjectionRepository;
import com.project.booking.repository.RecurringBookingRepository;
import com.project.common.enums.RecurringBookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
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
    private RecurringBookingOccurrenceEventPublisher occurrenceEventPublisher;

    @InjectMocks
    private RecurringBookingProcessor recurringBookingProcessor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recurringBookingProcessor, "generationWindowDays", 7);
    }

    @Test
    void processOneGeneratesMissingOccurrencesWithinWindow() {
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
        when(recurringBookingRepository.lockDueById(eq(recurringId), eq(RecurringBookingStatus.ACTIVE.name()), any()))
                .thenReturn(Optional.of(recurringBooking));
        when(bookingRepository.findGeneratedBookingDates(eq(recurringId), any(), any())).thenReturn(List.of());
        when(occurrenceEventPublisher.publishRequested(eq(recurringBooking), any(), eq(60))).thenReturn(true);

        recurringBookingProcessor.processOne(recurringId);

        assertEquals(LocalDateTime.now().plusDays(7).toLocalDate().atStartOfDay(), recurringBooking.getNextProcessAt());
        verify(occurrenceEventPublisher, times(3)).publishRequested(eq(recurringBooking), any(), eq(60));
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
        when(recurringBookingRepository.lockDueById(eq(recurringId), eq(RecurringBookingStatus.ACTIVE.name()), any()))
                .thenReturn(Optional.of(recurringBooking));
        when(bookingRepository.findGeneratedBookingDates(eq(recurringId), any(), any())).thenReturn(List.of());
        when(occurrenceEventPublisher.publishRequested(eq(recurringBooking), any(), eq(60))).thenReturn(true);
        when(recurringBookingRepository.existsBySubFieldIdAndStatus(subFieldId, RecurringBookingStatus.ACTIVE))
                .thenReturn(false);

        recurringBookingProcessor.processOne(recurringId);

        assertEquals(RecurringBookingStatus.COMPLETED, recurringBooking.getStatus());
        assertNull(recurringBooking.getNextProcessAt());
        verify(occurrenceEventPublisher).publishRequested(eq(recurringBooking), any(), eq(60));
        verify(subFieldRepository).updateHasRecurring(subFieldId, false);
        verify(recurringBookingRepository).save(recurringBooking);
    }

    @Test
    void processOneGeneratesPositiveDurationForAcrossMidnightBooking() {
        UUID recurringId = UUID.randomUUID();
        LocalDateTime nextProcessAt = LocalDateTime.now().minusMinutes(1);
        RecurringBooking recurringBooking = RecurringBooking.builder()
                .id(recurringId)
                .userId(UUID.randomUUID())
                .fieldId(UUID.randomUUID())
                .subFieldId(UUID.randomUUID())
                .startDate(nextProcessAt.toLocalDate())
                .endDate(nextProcessAt.toLocalDate().plusDays(10))
                .startTime(LocalTime.of(23, 0))
                .endTime(LocalTime.of(1, 0))
                .intervalDays(3)
                .status(RecurringBookingStatus.ACTIVE)
                .nextProcessAt(nextProcessAt)
                .build();
        when(recurringBookingRepository.lockDueById(eq(recurringId), eq(RecurringBookingStatus.ACTIVE.name()), any()))
                .thenReturn(Optional.of(recurringBooking));
        when(bookingRepository.findGeneratedBookingDates(eq(recurringId), any(), any())).thenReturn(List.of());
        when(occurrenceEventPublisher.publishRequested(eq(recurringBooking), any(), eq(120))).thenReturn(true);

        recurringBookingProcessor.processOne(recurringId);

        ArgumentCaptor<Integer> durationCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(occurrenceEventPublisher, times(3)).publishRequested(
                eq(recurringBooking),
                any(),
                durationCaptor.capture());
        assertEquals(120, durationCaptor.getAllValues().getFirst());
    }

    @Test
    void processOneSkipsDatesAlreadyGeneratedInWindow() {
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
        when(recurringBookingRepository.lockDueById(eq(recurringId), eq(RecurringBookingStatus.ACTIVE.name()), any()))
                .thenReturn(Optional.of(recurringBooking));
        when(bookingRepository.findGeneratedBookingDates(eq(recurringId), any(), any()))
                .thenReturn(List.of(nextProcessAt.toLocalDate().plusDays(3)));
        when(occurrenceEventPublisher.publishRequested(eq(recurringBooking), any(), eq(60))).thenReturn(true);

        recurringBookingProcessor.processOne(recurringId);

        verify(occurrenceEventPublisher, times(2)).publishRequested(eq(recurringBooking), any(), eq(60));
    }
}
