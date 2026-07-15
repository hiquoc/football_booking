package com.project.booking.service.impl;

import com.project.booking.cache.AvailabilityCacheService;
import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.request.CreateRecurringBookingRequest;
import com.project.booking.dto.request.UpdateRecurringBookingRequest;
import com.project.booking.dto.response.RecurringBookingResponse;
import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.entity.RecurringBooking;
import com.project.booking.mapper.RecurringBookingMapper;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.BookingSubFieldProjectionRepository;
import com.project.booking.repository.RecurringBookingRepository;
import com.project.booking.service.BookingService;
import com.project.booking.service.RecurringBookingService;
import com.project.booking.service.ResolvedOperatingHours;
import com.project.booking.service.SubFieldProjectionService;
import com.project.common.dto.PageResponse;
import com.project.common.enums.BookingStatus;
import com.project.common.enums.PaymentMethod;
import com.project.common.enums.RecurringBookingStatus;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.ConflictException;
import com.project.common.exception.ForbiddenException;
import com.project.common.exception.NotFoundException;
import com.project.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringBookingServiceImpl implements RecurringBookingService {

    private static final String ELIGIBILITY_MESSAGE =
            "You must complete at least one booking at this field before creating recurring bookings.";

    private final RecurringBookingRepository recurringBookingRepository;
    private final BookingRepository bookingRepository;
    private final BookingSubFieldProjectionRepository subFieldRepository;
    private final SubFieldProjectionService subFieldProjectionService;
    private final RecurringBookingMapper recurringBookingMapper;
    private final BookingService bookingService;
    private final AvailabilityCacheService availabilityCacheService;

    @Value("${booking.recurring-generation-lead-days:2}")
    private int generationLeadDays = 2;

    @Override
    @Transactional
    public RecurringBookingResponse create(UUID userId, CreateRecurringBookingRequest request) {
        SubFieldResponse subField = subFieldProjectionService.getRequiredSubField(request.getSubFieldId());
        validateRule(request.getDayOfWeek(), request.getStartTime(), request.getEndTime(), request.getStartDate(), request.getEndDate(), subField);
        validateEligibility(userId, subField.getFieldId());
        validateNoRecurringConflicts(userId, request.getSubFieldId(), request.getDayOfWeek(),
                request.getStartTime(), request.getEndTime(), request.getStartDate(), request.getEndDate(), null);

        RecurringBooking recurringBooking = RecurringBooking.builder()
                .userId(userId)
                .fieldId(subField.getFieldId())
                .subFieldId(subField.getId())
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(RecurringBookingStatus.ACTIVE)
                .nextProcessAt(nextProcessAt(request.getStartDate(), request.getDayOfWeek()))
                .build();

        RecurringBooking saved = recurringBookingRepository.save(recurringBooking);
        refreshHasRecurring(saved.getSubFieldId());
        availabilityCacheService.evictAll();
        return recurringBookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RecurringBookingResponse update(UUID userId, UUID id, UpdateRecurringBookingRequest request) {
        RecurringBooking recurringBooking = getOwned(userId, id);
        UUID previousSubFieldId = recurringBooking.getSubFieldId();
        SubFieldResponse subField = subFieldProjectionService.getRequiredSubField(request.getSubFieldId());
        validateRule(request.getDayOfWeek(), request.getStartTime(), request.getEndTime(), request.getStartDate(), request.getEndDate(), subField);
        validateNoRecurringConflicts(userId, request.getSubFieldId(), request.getDayOfWeek(),
                request.getStartTime(), request.getEndTime(), request.getStartDate(), request.getEndDate(), id);

        recurringBooking.setFieldId(subField.getFieldId());
        recurringBooking.setSubFieldId(subField.getId());
        recurringBooking.setDayOfWeek(request.getDayOfWeek());
        recurringBooking.setStartTime(request.getStartTime());
        recurringBooking.setEndTime(request.getEndTime());
        recurringBooking.setStartDate(request.getStartDate());
        recurringBooking.setEndDate(request.getEndDate());
        recurringBooking.setNextProcessAt(nextProcessAt(request.getStartDate(), request.getDayOfWeek()));

        RecurringBooking saved = recurringBookingRepository.save(recurringBooking);
        refreshHasRecurring(previousSubFieldId);
        refreshHasRecurring(saved.getSubFieldId());
        availabilityCacheService.evictAll();
        return recurringBookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RecurringBookingResponse pause(UUID userId, UUID id) {
        return changeOwnedStatus(userId, id, RecurringBookingStatus.PAUSED);
    }

    @Override
    @Transactional
    public RecurringBookingResponse resume(UUID userId, UUID id) {
        RecurringBooking recurringBooking = getOwned(userId, id);
        recurringBooking.setStatus(RecurringBookingStatus.ACTIVE);
        recurringBooking.setNextProcessAt(nextProcessAt(LocalDate.now(), recurringBooking.getDayOfWeek()));
        RecurringBooking saved = recurringBookingRepository.save(recurringBooking);
        refreshHasRecurring(saved.getSubFieldId());
        availabilityCacheService.evictAll();
        return recurringBookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RecurringBookingResponse cancel(UUID userId, UUID id) {
        return changeOwnedStatus(userId, id, RecurringBookingStatus.CANCELLED);
    }

    @Override
    @Transactional
    public RecurringBookingResponse adminPause(UUID id) {
        return changeAdminStatus(id, RecurringBookingStatus.PAUSED);
    }

    @Override
    @Transactional
    public RecurringBookingResponse adminResume(UUID id) {
        RecurringBooking recurringBooking = getRequired(id);
        recurringBooking.setStatus(RecurringBookingStatus.ACTIVE);
        recurringBooking.setNextProcessAt(nextProcessAt(LocalDate.now(), recurringBooking.getDayOfWeek()));
        RecurringBooking saved = recurringBookingRepository.save(recurringBooking);
        refreshHasRecurring(saved.getSubFieldId());
        availabilityCacheService.evictAll();
        return recurringBookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public RecurringBookingResponse adminCancel(UUID id) {
        return changeAdminStatus(id, RecurringBookingStatus.CANCELLED);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecurringBookingResponse> getMine(UUID userId, RecurringBookingStatus status, Pageable pageable) {
        var page = status == null
                ? recurringBookingRepository.findByUserId(userId, pageable)
                : recurringBookingRepository.findByUserIdAndStatus(userId, status, pageable);
        return PageResponse.from(page.map(recurringBookingMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecurringBookingResponse> getForOwner(UUID ownerId, RecurringBookingStatus status, Pageable pageable) {
        var page = status == null
                ? recurringBookingRepository.findBySubFieldOwnerId(ownerId, pageable)
                : recurringBookingRepository.findBySubFieldOwnerIdAndStatus(ownerId, status, pageable);
        return PageResponse.from(page.map(recurringBookingMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecurringBookingResponse> getForAdmin(RecurringBookingStatus status, Pageable pageable) {
        var page = status == null
                ? recurringBookingRepository.findAll(pageable)
                : recurringBookingRepository.findByStatus(status, pageable);
        return PageResponse.from(page.map(recurringBookingMapper::toResponse));
    }

    @Override
    public void processDue(LocalDateTime now) {
        recurringBookingRepository
                .findByStatusAndNextProcessAtLessThanEqualOrderByNextProcessAtAsc(RecurringBookingStatus.ACTIVE, now)
                .forEach(recurringBooking -> {
                    try {
                        processOne(recurringBooking.getId());
                    } catch (RuntimeException ex) {
                        log.error("Failed to process recurring booking id={}", recurringBooking.getId(), ex);
                    }
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(UUID id) {
        RecurringBooking recurringBooking = getRequired(id);
        if (recurringBooking.getStatus() != RecurringBookingStatus.ACTIVE
                || recurringBooking.getNextProcessAt().isAfter(LocalDateTime.now())) {
            return;
        }
        LocalDate playDate = recurringBooking.getNextProcessAt().toLocalDate().plusDays(generationLeadDays);
        if (playDate.isAfter(recurringBooking.getEndDate())) {
            recurringBooking.setStatus(RecurringBookingStatus.CANCELLED);
            recurringBookingRepository.save(recurringBooking);
            refreshHasRecurring(recurringBooking.getSubFieldId());
            return;
        }
        if (!bookingRepository.existsBySourceRecurringBookingIdAndBookingDate(recurringBooking.getId(), playDate)) {
            int durationMinutes = (int) java.time.Duration.between(
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
                            .paymentMethod(PaymentMethod.STRIPE)
                            .note("Generated from recurring booking " + recurringBooking.getId())
                            .build());
        }
        recurringBooking.setNextProcessAt(recurringBooking.getNextProcessAt().plusDays(7));
        recurringBookingRepository.save(recurringBooking);
    }

    private RecurringBookingResponse changeOwnedStatus(UUID userId, UUID id, RecurringBookingStatus status) {
        RecurringBooking recurringBooking = getOwned(userId, id);
        recurringBooking.setStatus(status);
        RecurringBooking saved = recurringBookingRepository.save(recurringBooking);
        refreshHasRecurring(saved.getSubFieldId());
        availabilityCacheService.evictAll();
        return recurringBookingMapper.toResponse(saved);
    }

    private RecurringBookingResponse changeAdminStatus(UUID id, RecurringBookingStatus status) {
        RecurringBooking recurringBooking = getRequired(id);
        recurringBooking.setStatus(status);
        RecurringBooking saved = recurringBookingRepository.save(recurringBooking);
        refreshHasRecurring(saved.getSubFieldId());
        availabilityCacheService.evictAll();
        return recurringBookingMapper.toResponse(saved);
    }

    private RecurringBooking getOwned(UUID userId, UUID id) {
        RecurringBooking recurringBooking = getRequired(id);
        if (!recurringBooking.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorised to manage this recurring booking");
        }
        return recurringBooking;
    }

    private RecurringBooking getRequired(UUID id) {
        return recurringBookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Recurring booking not found with id: " + id));
    }

    private void validateRule(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                              LocalDate startDate, LocalDate endDate, SubFieldResponse subField) {
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("End time must be after start time");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("End date must be on or after start date");
        }
        LocalDate firstOccurrence = firstOccurrenceOnOrAfter(startDate, dayOfWeek);
        if (firstOccurrence.isAfter(endDate)) {
            throw new BadRequestException("Date range must include the selected weekday");
        }
        ResolvedOperatingHours hours = subFieldProjectionService.resolveOperatingHours(subField.getId(), dayOfWeek);
        if (hours.closed() || hours.openTime() == null || hours.closeTime() == null
                || startTime.isBefore(hours.openTime()) || endTime.isAfter(hours.closeTime())) {
            throw new BadRequestException("Recurring booking time must be within operating hours");
        }
    }

    private void validateEligibility(UUID userId, UUID fieldId) {
        boolean eligible = bookingRepository.existsCompletedBookingAtField(userId, fieldId, BookingStatus.COMPLETED);
        if (!eligible) {
            throw new ForbiddenException(ELIGIBILITY_MESSAGE);
        }
    }

    private void validateNoRecurringConflicts(UUID userId, UUID subFieldId, DayOfWeek dayOfWeek,
                                              LocalTime startTime, LocalTime endTime,
                                              LocalDate startDate, LocalDate endDate, UUID excludeId) {
        boolean userOverlap = recurringBookingRepository.existsUserOverlap(
                userId, dayOfWeek, startTime, endTime, startDate, endDate, RecurringBookingStatus.ACTIVE, excludeId);
        boolean subFieldOverlap = recurringBookingRepository.existsSubFieldOverlap(
                subFieldId, dayOfWeek, startTime, endTime, startDate, endDate, RecurringBookingStatus.ACTIVE, excludeId);
        if (userOverlap || subFieldOverlap) {
            throw new ConflictException("Recurring booking overlaps an existing recurring booking.");
        }
    }

    private LocalDateTime nextProcessAt(LocalDate startDate, DayOfWeek dayOfWeek) {
        return firstOccurrenceOnOrAfter(startDate, dayOfWeek)
                .minusDays(generationLeadDays)
                .atStartOfDay();
    }

    private LocalDate firstOccurrenceOnOrAfter(LocalDate startDate, DayOfWeek dayOfWeek) {
        return startDate.getDayOfWeek() == dayOfWeek
                ? startDate
                : startDate.with(TemporalAdjusters.next(dayOfWeek));
    }

    private void refreshHasRecurring(UUID subFieldId) {
        boolean hasRecurring = recurringBookingRepository.existsBySubFieldIdAndStatus(subFieldId, RecurringBookingStatus.ACTIVE);
        subFieldRepository.updateHasRecurring(subFieldId, hasRecurring);
    }
}
