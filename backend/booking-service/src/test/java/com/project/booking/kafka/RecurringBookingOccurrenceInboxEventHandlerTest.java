package com.project.booking.kafka;

import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.events.RecurringBookingEventTopics;
import com.project.booking.events.RecurringBookingOccurrenceRequestedEvent;
import com.project.booking.exception.BookingConflictException;
import com.project.booking.repository.BookingRepository;
import com.project.booking.service.BookingService;
import com.project.common.enums.PaymentMethod;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.service.InboxService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecurringBookingOccurrenceInboxEventHandlerTest {

    @Test
    void supportsRecurringOccurrenceTopic() {
        RecurringBookingOccurrenceInboxEventHandler handler = handler(
                mock(InboxService.class),
                mock(BookingRepository.class),
                mock(BookingService.class));

        assertTrue(handler.supports(RecurringBookingEventTopics.RECURRING_OCCURRENCE_REQUESTED));
    }

    @Test
    void skipsWhenOccurrenceAlreadyHasBooking() {
        InboxService inboxService = mock(InboxService.class);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        BookingService bookingService = mock(BookingService.class);
        RecurringBookingOccurrenceInboxEventHandler handler = handler(inboxService, bookingRepository, bookingService);
        InboxEvent inboxEvent = envelope();
        RecurringBookingOccurrenceRequestedEvent payload = payload();
        when(inboxService.payload(inboxEvent, RecurringBookingOccurrenceRequestedEvent.class)).thenReturn(payload);
        when(bookingRepository.existsBySourceRecurringBookingIdAndBookingDateAndStatusIn(
                eq(payload.recurringBookingId()),
                eq(payload.bookingDate()),
                any())).thenReturn(true);

        handler.handle(inboxEvent);

        verify(bookingService, never()).createRecurringOccurrence(any(), any(), any());
    }

    @Test
    void createsRecurringOccurrenceWhenAvailable() {
        InboxService inboxService = mock(InboxService.class);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        BookingService bookingService = mock(BookingService.class);
        RecurringBookingOccurrenceInboxEventHandler handler = handler(inboxService, bookingRepository, bookingService);
        InboxEvent inboxEvent = envelope();
        RecurringBookingOccurrenceRequestedEvent payload = payload();
        when(inboxService.payload(inboxEvent, RecurringBookingOccurrenceRequestedEvent.class)).thenReturn(payload);
        when(bookingRepository.existsBySourceRecurringBookingIdAndBookingDateAndStatusIn(
                eq(payload.recurringBookingId()),
                eq(payload.bookingDate()),
                any())).thenReturn(false);

        handler.handle(inboxEvent);

        ArgumentCaptor<CreateBookingRequest> requestCaptor = ArgumentCaptor.forClass(CreateBookingRequest.class);
        verify(bookingService).createRecurringOccurrence(
                eq(payload.userId()),
                eq(payload.recurringBookingId()),
                requestCaptor.capture());
        assertEquals(payload.subFieldId(), requestCaptor.getValue().getSubFieldId());
        assertEquals(payload.bookingDate(), requestCaptor.getValue().getBookingDate());
        assertEquals(payload.startTime(), requestCaptor.getValue().getStartTime());
        assertEquals(payload.durationMinutes(), requestCaptor.getValue().getDurationMinutes());
        assertEquals(PaymentMethod.ACCOUNT_BALANCE, requestCaptor.getValue().getPaymentMethod());
    }

    @Test
    void treatsBookingConflictAsProcessedSkip() {
        InboxService inboxService = mock(InboxService.class);
        BookingRepository bookingRepository = mock(BookingRepository.class);
        BookingService bookingService = mock(BookingService.class);
        RecurringBookingOccurrenceInboxEventHandler handler = handler(inboxService, bookingRepository, bookingService);
        InboxEvent inboxEvent = envelope();
        RecurringBookingOccurrenceRequestedEvent payload = payload();
        when(inboxService.payload(inboxEvent, RecurringBookingOccurrenceRequestedEvent.class)).thenReturn(payload);
        when(bookingRepository.existsBySourceRecurringBookingIdAndBookingDateAndStatusIn(
                eq(payload.recurringBookingId()),
                eq(payload.bookingDate()),
                any())).thenReturn(false);
        doThrow(new BookingConflictException("occupied"))
                .when(bookingService).createRecurringOccurrence(eq(payload.userId()), eq(payload.recurringBookingId()), any());

        handler.handle(inboxEvent);

        verify(bookingService).createRecurringOccurrence(eq(payload.userId()), eq(payload.recurringBookingId()), any());
    }

    private RecurringBookingOccurrenceInboxEventHandler handler(
            InboxService inboxService,
            BookingRepository bookingRepository,
            BookingService bookingService) {
        return new RecurringBookingOccurrenceInboxEventHandler(inboxService, bookingRepository, bookingService);
    }

    private InboxEvent envelope() {
        return InboxEvent.builder()
                .topic(RecurringBookingEventTopics.RECURRING_OCCURRENCE_REQUESTED)
                .build();
    }

    private RecurringBookingOccurrenceRequestedEvent payload() {
        return new RecurringBookingOccurrenceRequestedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.now().plusDays(1),
                LocalTime.of(23, 0),
                120,
                Instant.now());
    }
}
