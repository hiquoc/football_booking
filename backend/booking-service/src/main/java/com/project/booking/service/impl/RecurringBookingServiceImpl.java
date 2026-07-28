package com.project.booking.service.impl;

import com.project.booking.cache.AvailabilityCacheService;
import com.project.booking.dto.request.CancelBookingRequest;
import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.request.CreateRecurringBookingRequest;
import com.project.booking.dto.request.UpdateRecurringBookingRequest;
import com.project.booking.dto.response.BookingResponse;
import com.project.booking.dto.response.RecurringBookingResponse;
import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.entity.RecurringBooking;
import com.project.booking.mapper.BookingMapper;
import com.project.booking.mapper.RecurringBookingMapper;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.BookingSubFieldProjectionRepository;
import com.project.booking.repository.RecurringBookingRepository;
import com.project.booking.service.BookingService;
import com.project.booking.service.RecurringBookingService;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringBookingServiceImpl implements RecurringBookingService {

    private static final String ELIGIBILITY_MESSAGE =
            "You must complete at least one booking at this field before creating recurring bookings.";
    private static final String IMMUTABLE_UPDATE_MESSAGE =
            "Only the recurring booking end date can be changed.";

    private final RecurringBookingRepository recurringBookingRepository;
    private final BookingRepository bookingRepository;
    private final BookingSubFieldProjectionRepository subFieldRepository;
    private final SubFieldProjectionService subFieldProjectionService;
    private final RecurringBookingMapper recurringBookingMapper;
    private final BookingMapper bookingMapper;
    private final BookingService bookingService;
    private final AvailabilityCacheService availabilityCacheService;

    @Value("${booking.recurring-generation-lead-days:2}")
    private int generationLeadDays = 2;

    @Override
    @Transactional
    public RecurringBookingResponse create(UUID userId, CreateRecurringBookingRequest request) {
        SubFieldResponse subField = subFieldProjectionService.getRequiredSubField(request.getSubFieldId());
        validateRule(request.getStartTime(), request.getEndTime(), request.getStartDate(), request.getEndDate(),
                request.getIntervalDays());
        validateEligibility(userId, subField.getFieldId());
        List<CreateBookingRequest> occurrences = generateOccurrences(request.getSubFieldId(), request.getStartTime(),
                request.getEndTime(), request.getStartDate(), request.getEndDate(), request.getIntervalDays());
        RecurringBooking replayed = findExactActiveRule(userId, request).orElse(null);
        if (replayed != null) {
            return toResponseWithFirstBooking(replayed,
                    bookingRepository.findFirstBySourceRecurringBookingIdOrderByStartDateTimeAsc(replayed.getId())
                            .map(bookingMapper::toResponse)
                            .orElse(null));
        }
        validateNoRecurringConflicts(userId, request.getSubFieldId(), request.getStartTime(), request.getEndTime(),
                request.getStartDate(), request.getEndDate(), request.getIntervalDays(), null);
        validateOccurrences(userId, occurrences, null);

        LocalDate firstOccurrenceDate = occurrences.getFirst().getBookingDate();
        LocalDate nextOccurrenceDate = firstOccurrenceDate.plusDays(request.getIntervalDays());
        boolean completedAfterFirstBooking = nextOccurrenceDate.isAfter(request.getEndDate());

        RecurringBooking recurringBooking = RecurringBooking.builder()
                .userId(userId)
                .fieldId(subField.getFieldId())
                .subFieldId(subField.getId())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .intervalDays(request.getIntervalDays())
                .status(completedAfterFirstBooking ? RecurringBookingStatus.COMPLETED : RecurringBookingStatus.ACTIVE)
                .nextProcessAt(completedAfterFirstBooking ? null : nextProcessAt(nextOccurrenceDate))
                .build();

        RecurringBooking saved = recurringBookingRepository.save(recurringBooking);
        BookingResponse firstBooking = bookingService.createRecurringOccurrence(
                userId,
                saved.getId(),
                CreateBookingRequest.builder()
                        .subFieldId(saved.getSubFieldId())
                        .bookingDate(firstOccurrenceDate)
                        .startTime(saved.getStartTime())
                        .durationMinutes((int) Duration.between(saved.getStartTime(), saved.getEndTime()).toMinutes())
                        .paymentMethod(PaymentMethod.ACCOUNT_BALANCE)
                        .note("Đặt sân định kì " + saved.getId())
                        .build());
        refreshHasRecurring(saved.getSubFieldId());
        availabilityCacheService.evictAll();
        return toResponseWithFirstBooking(saved, firstBooking);
    }

    @Override
    @Transactional
    public RecurringBookingResponse update(UUID userId, UUID id, UpdateRecurringBookingRequest request) {
        RecurringBooking recurringBooking = getOwned(userId, id);
        validateEndDateOnlyUpdate(recurringBooking, request);
        validateEndDate(recurringBooking.getStartDate(), request.getEndDate());
        validateNewOccurrencesForEndDate(userId, recurringBooking, request.getEndDate());
        recurringBooking.setEndDate(request.getEndDate());

        RecurringBooking saved = recurringBookingRepository.save(recurringBooking);
        refreshHasRecurring(saved.getSubFieldId());
        availabilityCacheService.evictAll();
        return toResponseWithLatestBooking(saved);
    }

    @Override
    @Transactional
    public RecurringBookingResponse pause(UUID userId, UUID id) {
        return changeOwnedStatus(userId, id, RecurringBookingStatus.PAUSED, false);
    }

    @Override
    @Transactional
    public RecurringBookingResponse resume(UUID userId, UUID id) {
        RecurringBooking recurringBooking = getOwned(userId, id);
        validateCanResume(recurringBooking);
        recurringBooking.setStatus(RecurringBookingStatus.ACTIVE);
        recurringBooking.setNextProcessAt(nextProcessAt(nextOccurrenceOnOrAfter(recurringBooking, LocalDate.now())));
        RecurringBooking saved = recurringBookingRepository.save(recurringBooking);
        refreshHasRecurring(saved.getSubFieldId());
        availabilityCacheService.evictAll();
        return toResponseWithLatestBooking(saved);
    }

    @Override
    @Transactional
    public RecurringBookingResponse cancel(UUID userId, UUID id) {
        return changeOwnedStatus(userId, id, RecurringBookingStatus.CANCELLED, true);
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
        validateCanResume(recurringBooking);
        recurringBooking.setStatus(RecurringBookingStatus.ACTIVE);
        recurringBooking.setNextProcessAt(nextProcessAt(nextOccurrenceOnOrAfter(recurringBooking, LocalDate.now())));
        RecurringBooking saved = recurringBookingRepository.save(recurringBooking);
        refreshHasRecurring(saved.getSubFieldId());
        availabilityCacheService.evictAll();
        return toResponseWithLatestBooking(saved);
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
        return PageResponse.from(page.map(this::toResponseWithLatestBooking));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecurringBookingResponse> getForOwner(UUID ownerId, RecurringBookingStatus status, Pageable pageable) {
        var page = status == null
                ? recurringBookingRepository.findBySubFieldOwnerId(ownerId, pageable)
                : recurringBookingRepository.findBySubFieldOwnerIdAndStatus(ownerId, status, pageable);
        return PageResponse.from(page.map(this::toResponseWithLatestBooking));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecurringBookingResponse> getForAdmin(RecurringBookingStatus status, Pageable pageable) {
        var page = status == null
                ? recurringBookingRepository.findAll(pageable)
                : recurringBookingRepository.findByStatus(status, pageable);
        return PageResponse.from(page.map(this::toResponseWithLatestBooking));
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
                || recurringBooking.getNextProcessAt() == null
                || recurringBooking.getNextProcessAt().isAfter(LocalDateTime.now())) {
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

    private RecurringBookingResponse changeOwnedStatus(UUID userId, UUID id, RecurringBookingStatus status, boolean cancelLatestConfirmedBooking) {
        RecurringBooking recurringBooking = getOwned(userId, id);
        if (cancelLatestConfirmedBooking) {
            cancelLatestConfirmedBooking(userId, recurringBooking);
        }
        recurringBooking.setStatus(status);
        RecurringBooking saved = recurringBookingRepository.save(recurringBooking);
        refreshHasRecurring(saved.getSubFieldId());
        availabilityCacheService.evictAll();
        return toResponseWithLatestBooking(saved);
    }

    private RecurringBookingResponse changeAdminStatus(UUID id, RecurringBookingStatus status) {
        RecurringBooking recurringBooking = getRequired(id);
        recurringBooking.setStatus(status);
        RecurringBooking saved = recurringBookingRepository.save(recurringBooking);
        refreshHasRecurring(saved.getSubFieldId());
        availabilityCacheService.evictAll();
        return toResponseWithLatestBooking(saved);
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

    private void validateRule(LocalTime startTime, LocalTime endTime, LocalDate startDate, LocalDate endDate,
                              Integer intervalDays) {
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("End time must be after start time");
        }
        if (!endDate.isAfter(startDate)) {
            throw new BadRequestException("End date must be after start date");
        }
        if (intervalDays == null || intervalDays < 1 || intervalDays > 7) {
            throw new BadRequestException("Interval days must be between 1 and 7");
        }
        if (generateOccurrenceDates(startDate, endDate, intervalDays).isEmpty()) {
            throw new BadRequestException("Recurring booking must generate at least one occurrence");
        }
    }

    private void validateEligibility(UUID userId, UUID fieldId) {
        boolean eligible = bookingRepository.existsCompletedBookingAtField(userId, fieldId, BookingStatus.COMPLETED);
        if (!eligible) {
            throw new ForbiddenException(ELIGIBILITY_MESSAGE);
        }
    }

    private void validateNoRecurringConflicts(UUID userId, UUID subFieldId, LocalTime startTime, LocalTime endTime,
                                              LocalDate startDate, LocalDate endDate, int intervalDays, UUID excludeId) {
        boolean userOverlap = recurringBookingRepository.overlapsAnyGeneratedOccurrence(
                recurringBookingRepository.findUserOverlapCandidates(
                        userId, startTime, endTime, startDate, endDate, RecurringBookingStatus.ACTIVE, excludeId),
                startDate, endDate, intervalDays);
        boolean subFieldOverlap = recurringBookingRepository.overlapsAnyGeneratedOccurrence(
                recurringBookingRepository.findSubFieldOverlapCandidates(
                        subFieldId, startTime, endTime, startDate, endDate, RecurringBookingStatus.ACTIVE, excludeId),
                startDate, endDate, intervalDays);
        if (userOverlap || subFieldOverlap) {
            throw new ConflictException("Recurring booking overlaps an existing recurring booking.");
        }
    }

    private void validateEndDateOnlyUpdate(RecurringBooking recurringBooking, UpdateRecurringBookingRequest request) {
        if (!recurringBooking.getSubFieldId().equals(request.getSubFieldId())
                || !recurringBooking.getStartTime().equals(request.getStartTime())
                || !recurringBooking.getEndTime().equals(request.getEndTime())
                || !recurringBooking.getStartDate().equals(request.getStartDate())
                || !recurringBooking.getIntervalDays().equals(request.getIntervalDays())) {
            throw new BadRequestException(IMMUTABLE_UPDATE_MESSAGE);
        }
    }

    private void validateEndDate(LocalDate startDate, LocalDate endDate) {
        if (endDate == null) {
            throw new BadRequestException("End date is required");
        }
        if (endDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("End date must be today or in the future");
        }
        if (!endDate.isAfter(startDate)) {
            throw new BadRequestException("End date must be after start date");
        }
    }

    private void validateNewOccurrencesForEndDate(UUID userId, RecurringBooking recurringBooking, LocalDate endDate) {
        if (!endDate.isAfter(recurringBooking.getEndDate())) {
            return;
        }
        LocalDate firstNewDate = recurringBooking.getEndDate().plusDays(recurringBooking.getIntervalDays());
        if (firstNewDate.isAfter(endDate)) {
            return;
        }
        List<CreateBookingRequest> newOccurrences = generateOccurrences(
                recurringBooking.getSubFieldId(),
                recurringBooking.getStartTime(),
                recurringBooking.getEndTime(),
                firstNewDate,
                endDate,
                recurringBooking.getIntervalDays());
        validateNoRecurringConflicts(userId, recurringBooking.getSubFieldId(), recurringBooking.getStartTime(),
                recurringBooking.getEndTime(), firstNewDate, endDate, recurringBooking.getIntervalDays(), recurringBooking.getId());
        validateOccurrences(userId, newOccurrences, recurringBooking.getId());
    }

    private void cancelLatestConfirmedBooking(UUID userId, RecurringBooking recurringBooking) {
        bookingRepository.findFirstBySourceRecurringBookingIdOrderByStartDateTimeDesc(recurringBooking.getId())
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                .ifPresent(booking -> bookingService.cancelBooking(userId, CancelBookingRequest.builder()
                        .bookingId(booking.getId())
                        .reason("Recurring booking cancelled")
                        .build()));
    }

    private void validateCanResume(RecurringBooking recurringBooking) {
        if (recurringBooking.getStatus() == RecurringBookingStatus.COMPLETED) {
            throw new BadRequestException("Completed recurring bookings cannot be resumed");
        }
    }

    private void completeRecurringBooking(RecurringBooking recurringBooking) {
        recurringBooking.setStatus(RecurringBookingStatus.COMPLETED);
        recurringBooking.setNextProcessAt(null);
    }

    private Optional<RecurringBooking> findExactActiveRule(UUID userId, CreateRecurringBookingRequest request) {
        return recurringBookingRepository
                .findFirstByUserIdAndSubFieldIdAndStartTimeAndEndTimeAndStartDateAndEndDateAndIntervalDaysAndStatus(
                        userId,
                        request.getSubFieldId(),
                        request.getStartTime(),
                        request.getEndTime(),
                        request.getStartDate(),
                        request.getEndDate(),
                        request.getIntervalDays(),
                        RecurringBookingStatus.ACTIVE);
    }

    private RecurringBookingResponse toResponseWithFirstBooking(RecurringBooking recurringBooking, BookingResponse firstBooking) {
        RecurringBookingResponse response = toResponseWithLatestBooking(recurringBooking);
        response.setFirstBooking(firstBooking);
        return response;
    }

    private RecurringBookingResponse toResponseWithLatestBooking(RecurringBooking recurringBooking) {
        RecurringBookingResponse response = recurringBookingMapper.toResponse(recurringBooking);
        response.setNextMatchAt(nextMatchAt(recurringBooking));
        Optional.ofNullable(bookingRepository.findFirstBySourceRecurringBookingIdOrderByStartDateTimeDesc(recurringBooking.getId()))
                .flatMap(optional -> optional)
                .map(bookingMapper::toResponse)
                .ifPresent(response::setLatestBooking);
        return response;
    }

    private void validateOccurrences(UUID userId, List<CreateBookingRequest> occurrences, UUID recurringBookingId) {
        occurrences.forEach(occurrence -> bookingService.validateRecurringOccurrence(userId, occurrence, recurringBookingId));
    }

    private List<CreateBookingRequest> generateOccurrences(UUID subFieldId, LocalTime startTime, LocalTime endTime,
                                                           LocalDate startDate, LocalDate endDate, int intervalDays) {
        int durationMinutes = (int) Duration.between(startTime, endTime).toMinutes();
        return generateOccurrenceDates(startDate, endDate, intervalDays)
                .stream()
                .map(occurrenceDate -> CreateBookingRequest.builder()
                        .subFieldId(subFieldId)
                        .bookingDate(occurrenceDate)
                        .startTime(startTime)
                        .durationMinutes(durationMinutes)
                        .paymentMethod(PaymentMethod.ACCOUNT_BALANCE)
                        .build())
                .toList();
    }

    private List<LocalDate> generateOccurrenceDates(LocalDate startDate, LocalDate endDate, int intervalDays) {
        return startDate.datesUntil(endDate.plusDays(1), java.time.Period.ofDays(intervalDays)).toList();
    }

    private LocalDateTime nextProcessAt(LocalDate occurrenceDate) {
        return occurrenceDate.minusDays(generationLeadDays).atStartOfDay();
    }

    private LocalDateTime nextMatchAt(RecurringBooking recurringBooking) {
        if (recurringBooking.getNextProcessAt() == null) {
            return null;
        }
        LocalDate matchDate = recurringBooking.getNextProcessAt().toLocalDate().plusDays(generationLeadDays);
        if (matchDate.isAfter(recurringBooking.getEndDate())) {
            return null;
        }
        return LocalDateTime.of(matchDate, recurringBooking.getStartTime());
    }

    private LocalDate nextOccurrenceOnOrAfter(RecurringBooking recurringBooking, LocalDate date) {
        LocalDate occurrence = recurringBooking.getStartDate();
        while (occurrence.isBefore(date)) {
            occurrence = occurrence.plusDays(recurringBooking.getIntervalDays());
        }
        return occurrence;
    }

    private void refreshHasRecurring(UUID subFieldId) {
        boolean hasRecurring = recurringBookingRepository.existsBySubFieldIdAndStatus(subFieldId, RecurringBookingStatus.ACTIVE);
        subFieldRepository.updateHasRecurring(subFieldId, hasRecurring);
    }
}
