package com.project.booking.kafka;

import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.events.RecurringBookingEventTopics;
import com.project.booking.events.RecurringBookingOccurrenceRequestedEvent;
import com.project.booking.exception.BookingConflictException;
import com.project.booking.repository.BookingRepository;
import com.project.booking.service.BookingService;
import com.project.common.enums.BookingStatus;
import com.project.common.enums.PaymentMethod;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.handler.InboxEventHandler;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringBookingOccurrenceInboxEventHandler implements InboxEventHandler {

    private static final List<BookingStatus> RESERVING_STATUSES = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final InboxService inboxService;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @Override
    public boolean supports(String topic) {
        return RecurringBookingEventTopics.RECURRING_OCCURRENCE_REQUESTED.equals(topic);
    }

    @Override
    @Transactional
    public void handle(InboxEvent event) {
        RecurringBookingOccurrenceRequestedEvent payload = inboxService.payload(
                event,
                RecurringBookingOccurrenceRequestedEvent.class);
        if (bookingRepository.existsBySourceRecurringBookingIdAndBookingDateAndStatusIn(
                payload.recurringBookingId(),
                payload.bookingDate(),
                RESERVING_STATUSES)) {
            log.info("Skipped recurring occurrence command because booking already exists: recurringBookingId={}, date={}",
                    payload.recurringBookingId(), payload.bookingDate());
            return;
        }
        try {
            bookingService.createRecurringOccurrence(
                    payload.userId(),
                    payload.recurringBookingId(),
                    CreateBookingRequest.builder()
                            .subFieldId(payload.subFieldId())
                            .bookingDate(payload.bookingDate())
                            .startTime(payload.startTime())
                            .durationMinutes(payload.durationMinutes())
                            .paymentMethod(PaymentMethod.ACCOUNT_BALANCE)
                            .note("Được tạo từ lịch đặt sân định kì " + payload.recurringBookingId())
                            .build());
        } catch (BookingConflictException ex) {
            log.info("Skipped recurring occurrence command because slot is occupied: recurringBookingId={}, date={}",
                    payload.recurringBookingId(), payload.bookingDate());
        }
    }
}
