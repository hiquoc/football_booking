package com.project.booking.service;

import com.project.booking.entity.RecurringBooking;
import com.project.booking.kafka.RecurringBookingOccurrenceEventPublisher;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.BookingSubFieldProjectionRepository;
import com.project.booking.repository.RecurringBookingRepository;
import com.project.common.enums.RecurringBookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecurringBookingProcessor {

    private final RecurringBookingRepository recurringBookingRepository;
    private final BookingRepository bookingRepository;
    private final BookingSubFieldProjectionRepository subFieldRepository;
    private final RecurringBookingOccurrenceEventPublisher occurrenceEventPublisher;

    @Value("${booking.recurring-generation-window-days:7}")
    private int generationWindowDays = 7;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(UUID id) {
        LocalDateTime now = LocalDateTime.now();
        RecurringBooking recurringBooking = recurringBookingRepository
                .lockDueById(id, RecurringBookingStatus.ACTIVE.name(), now)
                .orElse(null);
        if (recurringBooking == null) {
            return;
        }

        LocalDate today = now.toLocalDate();
        if (recurringBooking.getEndDate().isBefore(today)) {
            recurringBooking.setStatus(RecurringBookingStatus.COMPLETED);
            recurringBooking.setNextProcessAt(null);
            recurringBookingRepository.save(recurringBooking);
            refreshHasRecurring(recurringBooking.getSubFieldId());
            return;
        }

        generateMissingOccurrences(recurringBooking, today);
        LocalDate nextOccurrenceDate = nextOccurrenceOnOrAfter(recurringBooking, today.plusDays(1));
        if (nextOccurrenceDate.isAfter(recurringBooking.getEndDate())) {
            completeRecurringBooking(recurringBooking);
            refreshHasRecurring(recurringBooking.getSubFieldId());
        } else {
            recurringBooking.setNextProcessAt(today.plusDays(generationWindowDays).atStartOfDay());
        }
        recurringBookingRepository.save(recurringBooking);
    }

    private void generateMissingOccurrences(RecurringBooking recurringBooking, LocalDate windowStart) {
        LocalDate windowEnd = windowStart.plusDays(generationWindowDays);
        Set<LocalDate> generatedDates = Set.copyOf(bookingRepository.findGeneratedBookingDates(
                recurringBooking.getId(),
                windowStart.atStartOfDay(),
                windowEnd.plusDays(1).atStartOfDay()));
        occurrenceDates(recurringBooking, windowStart, windowEnd).stream()
                .filter(occurrenceDate -> !generatedDates.contains(occurrenceDate))
                .forEach(occurrenceDate -> createOccurrenceIfAvailable(recurringBooking, occurrenceDate));
    }

    private boolean createOccurrenceIfAvailable(RecurringBooking recurringBooking, LocalDate occurrenceDate) {
        return occurrenceEventPublisher.publishRequested(
                recurringBooking,
                occurrenceDate,
                durationMinutes(recurringBooking));
    }

    private List<LocalDate> occurrenceDates(RecurringBooking recurringBooking, LocalDate windowStart, LocalDate windowEnd) {
        LocalDate firstDate = nextOccurrenceOnOrAfter(recurringBooking, windowStart);
        LocalDate boundedEnd = windowEnd.isBefore(recurringBooking.getEndDate()) ? windowEnd : recurringBooking.getEndDate();
        if (firstDate.isAfter(boundedEnd)) {
            return List.of();
        }
        return firstDate.datesUntil(boundedEnd.plusDays(1), java.time.Period.ofDays(recurringBooking.getIntervalDays())).toList();
    }

    private LocalDate nextOccurrenceOnOrAfter(RecurringBooking recurringBooking, LocalDate date) {
        LocalDate occurrence = recurringBooking.getStartDate();
        while (occurrence.isBefore(date)) {
            occurrence = occurrence.plusDays(recurringBooking.getIntervalDays());
        }
        return occurrence;
    }

    private int durationMinutes(RecurringBooking recurringBooking) {
        int minutes = (int) Duration.between(
                recurringBooking.getStartTime(),
                recurringBooking.getEndTime()).toMinutes();
        return minutes > 0 ? minutes : minutes + (24 * 60);
    }

    private void completeRecurringBooking(RecurringBooking recurringBooking) {
        recurringBooking.setStatus(RecurringBookingStatus.COMPLETED);
        recurringBooking.setNextProcessAt(null);
    }

    private void refreshHasRecurring(UUID subFieldId) {
        boolean hasRecurring = recurringBookingRepository.existsBySubFieldIdAndStatus(subFieldId, RecurringBookingStatus.ACTIVE);
        subFieldRepository.updateHasRecurring(subFieldId, hasRecurring);
    }
}
