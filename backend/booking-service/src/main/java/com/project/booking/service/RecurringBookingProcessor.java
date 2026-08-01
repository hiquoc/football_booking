package com.project.booking.service;

import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.entity.RecurringBooking;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.BookingSubFieldProjectionRepository;
import com.project.booking.repository.RecurringBookingRepository;
import com.project.common.enums.PaymentMethod;
import com.project.common.enums.RecurringBookingStatus;
import com.project.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecurringBookingProcessor {

    private final RecurringBookingRepository recurringBookingRepository;
    private final BookingRepository bookingRepository;
    private final BookingSubFieldProjectionRepository subFieldRepository;
    private final BookingService bookingService;

    @Value("${booking.recurring-generation-lead-days:2}")
    private int generationLeadDays = 2;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(UUID id) {
        RecurringBooking recurringBooking = getRequired(id);
        if (recurringBooking.getStatus() != RecurringBookingStatus.ACTIVE
                || recurringBooking.getNextProcessAt() == null
                || recurringBooking.getNextProcessAt().isAfter(LocalDateTime.now())) {
            return;
        }
        if(recurringBooking.getEndDate().isBefore(LocalDate.now())) {
            recurringBooking.setStatus(RecurringBookingStatus.COMPLETED);
            return;
        }
        LocalDate playDate = recurringBooking.getNextProcessAt().toLocalDate().plusDays(generationLeadDays);
        if (playDate.isAfter(recurringBooking.getEndDate())) {
            completeRecurringBooking(recurringBooking);
            recurringBookingRepository.save(recurringBooking);
            refreshHasRecurring(recurringBooking.getSubFieldId());
            return;
        }
        if (!bookingRepository.existsBySourceRecurringBookingIdAndBookingDate(recurringBooking.getId(), playDate)) {
            int durationMinutes = (int) Duration.between(
                    recurringBooking.getStartTime(),
                    recurringBooking.getEndTime()).toMinutes();
            bookingService.createRecurringOccurrence(
                    recurringBooking.getUserId(),
                    recurringBooking.getId(),
                    CreateBookingRequest.builder()
                            .subFieldId(recurringBooking.getSubFieldId())
                            .bookingDate(playDate)
                            .startTime(recurringBooking.getStartTime())
                            .durationMinutes(durationMinutes)
                            .paymentMethod(PaymentMethod.ACCOUNT_BALANCE)
                            .note("Generated from recurring booking " + recurringBooking.getId())
                            .build());
        }
        LocalDate nextOccurrenceDate = playDate.plusDays(recurringBooking.getIntervalDays());
        if (nextOccurrenceDate.isAfter(recurringBooking.getEndDate())) {
            completeRecurringBooking(recurringBooking);
            recurringBookingRepository.save(recurringBooking);
            refreshHasRecurring(recurringBooking.getSubFieldId());
        } else {
            recurringBooking.setNextProcessAt(nextProcessAt(nextOccurrenceDate));
            recurringBookingRepository.save(recurringBooking);
        }
    }

    private RecurringBooking getRequired(UUID id) {
        return recurringBookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Recurring booking not found with id: " + id));
    }

    private void completeRecurringBooking(RecurringBooking recurringBooking) {
        recurringBooking.setStatus(RecurringBookingStatus.COMPLETED);
        recurringBooking.setNextProcessAt(null);
    }

    private LocalDateTime nextProcessAt(LocalDate occurrenceDate) {
        return occurrenceDate.minusDays(generationLeadDays).atStartOfDay();
    }

    private void refreshHasRecurring(UUID subFieldId) {
        boolean hasRecurring = recurringBookingRepository.existsBySubFieldIdAndStatus(subFieldId, RecurringBookingStatus.ACTIVE);
        subFieldRepository.updateHasRecurring(subFieldId, hasRecurring);
    }
}
