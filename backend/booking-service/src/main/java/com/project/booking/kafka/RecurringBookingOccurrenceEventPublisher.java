package com.project.booking.kafka;

import com.project.booking.entity.RecurringBooking;
import com.project.booking.events.RecurringBookingEventTopics;
import com.project.booking.events.RecurringBookingOccurrenceRequestedEvent;
import com.project.common.outbox.dto.OutboxSaveRequest;
import com.project.common.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringBookingOccurrenceEventPublisher {

    private static final String AGGREGATE_TYPE = "RecurringBookingOccurrence";
    private static final String EVENT_TYPE = "RECURRING_OCCURRENCE_REQUESTED";

    private final OutboxService outboxService;

    public boolean publishRequested(RecurringBooking recurringBooking, LocalDate occurrenceDate, int durationMinutes) {
        String aggregateId = aggregateId(recurringBooking.getId().toString(), occurrenceDate);
        RecurringBookingOccurrenceRequestedEvent event = new RecurringBookingOccurrenceRequestedEvent(
                recurringBooking.getId(),
                recurringBooking.getUserId(),
                recurringBooking.getSubFieldId(),
                occurrenceDate,
                recurringBooking.getStartTime(),
                durationMinutes,
                Instant.now());
        try {
            outboxService.saveAndFlush(new OutboxSaveRequest(
                    AGGREGATE_TYPE,
                    aggregateId,
                    EVENT_TYPE,
                    RecurringBookingEventTopics.RECURRING_OCCURRENCE_REQUESTED,
                    recurringBooking.getSubFieldId().toString(),
                    event));
            return true;
        } catch (DataIntegrityViolationException ex) {
            log.info("Recurring occurrence command already exists: recurringBookingId={}, occurrenceDate={}",
                    recurringBooking.getId(), occurrenceDate);
            return false;
        }
    }

    private String aggregateId(String recurringBookingId, LocalDate occurrenceDate) {
        return recurringBookingId + ":" + occurrenceDate;
    }
}
