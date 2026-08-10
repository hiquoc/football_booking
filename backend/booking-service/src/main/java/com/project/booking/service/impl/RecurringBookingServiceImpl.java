package com.project.booking.service.impl;

import com.project.booking.cache.AvailabilityCacheService;
import com.project.booking.dto.request.CancelBookingRequest;
import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.request.CreateRecurringBookingRequest;
import com.project.booking.dto.request.UpdateRecurringBookingRequest;
import com.project.booking.dto.response.BookingResponse;
import com.project.booking.dto.response.RecurringBookingResponse;
import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.entity.Booking;
import com.project.booking.entity.RecurringBooking;
import com.project.booking.exception.BookingConflictException;
import com.project.booking.kafka.RecurringBookingOccurrenceEventPublisher;
import com.project.booking.mapper.BookingMapper;
import com.project.booking.mapper.RecurringBookingMapper;
import com.project.booking.moderation.service.BookingModerationService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecurringBookingServiceImpl implements RecurringBookingService {

    private static final List<BookingStatus> RESERVING_STATUSES = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
    private static final String ELIGIBILITY_MESSAGE =
            "You must complete at least one booking at this field before creating recurring bookings.";
    private static final String ELIGIBILITY_CODE = "RECURRING_BOOKING_COMPLETED_BOOKING_REQUIRED";
    private static final String IMMUTABLE_UPDATE_MESSAGE =
            "Only the recurring booking end date can be changed.";
    private static final String SUBFIELD_CLOSED_CODE = "SUBFIELD_CLOSED";
    private static final String RECURRING_SUBFIELD_CLOSED_ON_DATE_CODE = "RECURRING_SUBFIELD_CLOSED_ON_DATE";
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_RECURRING_BOOKING_YEARS = 1;

    private final RecurringBookingRepository recurringBookingRepository;
    private final BookingRepository bookingRepository;
    private final BookingSubFieldProjectionRepository subFieldRepository;
    private final SubFieldProjectionService subFieldProjectionService;
    private final RecurringBookingMapper recurringBookingMapper;
    private final BookingMapper bookingMapper;
    private final BookingService bookingService;
    private final BookingModerationService bookingModerationService;
    private final RecurringBookingOccurrenceEventPublisher occurrenceEventPublisher;
    private final AvailabilityCacheService availabilityCacheService;

    @Value("${booking.recurring-generation-window-days:7}")
    private int generationWindowDays = 7;

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
                .nextProcessAt(completedAfterFirstBooking ? null : LocalDateTime.now())
                .build();

        RecurringBooking saved = recurringBookingRepository.save(recurringBooking);
        BookingResponse firstBooking = bookingService.createRecurringOccurrence(
                userId,
                saved.getId(),
                CreateBookingRequest.builder()
                        .subFieldId(saved.getSubFieldId())
                        .bookingDate(firstOccurrenceDate)
                        .startTime(saved.getStartTime())
                        .durationMinutes(durationMinutes(saved.getStartTime(), saved.getEndTime()))
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
        return resumeRecurringBooking(recurringBooking);
    }

    @Override
    @Transactional
    public RecurringBookingResponse cancel(UUID userId, UUID id) {
        return changeOwnedStatus(userId, id, RecurringBookingStatus.CANCELLED, true);
    }

    @Override
    @Transactional
    public RecurringBookingResponse ownerPause(UUID ownerId, UUID id) {
        return changeOwnerStatus(ownerId, id, RecurringBookingStatus.PAUSED, false);
    }

    @Override
    @Transactional
    public RecurringBookingResponse ownerResume(UUID ownerId, UUID id) {
        RecurringBooking recurringBooking = getOwnedByFieldOwner(ownerId, id);
        return resumeRecurringBooking(recurringBooking);
    }

    @Override
    @Transactional
    public RecurringBookingResponse ownerCancel(UUID ownerId, UUID id) {
        return changeOwnerStatus(ownerId, id, RecurringBookingStatus.CANCELLED, true);
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
        return resumeRecurringBooking(recurringBooking);
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

    private RecurringBookingResponse changeOwnerStatus(UUID ownerId, UUID id, RecurringBookingStatus status, boolean cancelLatestConfirmedBooking) {
        RecurringBooking recurringBooking = getOwnedByFieldOwner(ownerId, id);
        if (cancelLatestConfirmedBooking) {
            cancelLatestConfirmedBookingByOwner(ownerId, recurringBooking);
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

    private RecurringBookingResponse resumeRecurringBooking(RecurringBooking recurringBooking) {
        validateCanResume(recurringBooking);
        ResumeWindowResult result = generateResumeWindow(recurringBooking);
        if (result.allOccurrencesOccupied()) {
            recurringBooking.setStatus(RecurringBookingStatus.PAUSED);
        } else {
            recurringBooking.setStatus(RecurringBookingStatus.ACTIVE);
            recurringBooking.setNextProcessAt(LocalDate.now().plusDays(1).atStartOfDay());
        }
        RecurringBooking saved = recurringBookingRepository.save(recurringBooking);
        refreshHasRecurring(saved.getSubFieldId());
        availabilityCacheService.evictAll();
        RecurringBookingResponse response = toResponseWithLatestBooking(saved);
        response.setGeneratedDates(result.generatedDates());
        response.setOccupiedDates(result.occupiedDates());
        return response;
    }

    private ResumeWindowResult generateResumeWindow(RecurringBooking recurringBooking) {
        List<LocalDate> occurrenceDates = occurrenceDatesInGenerationWindow(recurringBooking, LocalDate.now());
        List<Booking> futureBookings = bookingRepository.findOverlappingBookings(
                recurringBooking.getSubFieldId(),
                LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(generationWindowDays + 1L).atStartOfDay(),
                RESERVING_STATUSES);
        List<LocalDate> generatedDates = new ArrayList<>();
        List<LocalDate> occupiedDates = new ArrayList<>();
        int availableOccurrences = 0;
        for (LocalDate occurrenceDate : occurrenceDates) {
            if (isOccupiedByOtherBooking(recurringBooking, occurrenceDate, futureBookings)) {
                occupiedDates.add(occurrenceDate);
                continue;
            }
            if (alreadyHasActiveOccurrence(recurringBooking, occurrenceDate)) {
                availableOccurrences++;
                continue;
            }
            try {
                occurrenceEventPublisher.publishRequested(
                        recurringBooking,
                        occurrenceDate,
                        durationMinutes(recurringBooking.getStartTime(), recurringBooking.getEndTime()));
                generatedDates.add(occurrenceDate);
                availableOccurrences++;
            } catch (BookingConflictException ex) {
                occupiedDates.add(occurrenceDate);
            }
        }
        return new ResumeWindowResult(
                List.copyOf(generatedDates),
                List.copyOf(occupiedDates),
                !occurrenceDates.isEmpty() && availableOccurrences == 0);
    }

    private boolean alreadyHasActiveOccurrence(RecurringBooking recurringBooking, LocalDate occurrenceDate) {
        return bookingRepository.existsBySourceRecurringBookingIdAndBookingDateAndStatusIn(
                recurringBooking.getId(),
                occurrenceDate,
                RESERVING_STATUSES);
    }

    private boolean isOccupiedByOtherBooking(
            RecurringBooking recurringBooking,
            LocalDate occurrenceDate,
            List<Booking> futureBookings) {
        LocalDateTime occurrenceStart = LocalDateTime.of(occurrenceDate, recurringBooking.getStartTime());
        LocalDateTime occurrenceEnd = occurrenceStart.plusMinutes(
                durationMinutes(recurringBooking.getStartTime(), recurringBooking.getEndTime()));
        return futureBookings.stream()
                .filter(booking -> !recurringBooking.getId().equals(booking.getSourceRecurringBookingId()))
                .anyMatch(booking -> booking.getStartDateTime().isBefore(occurrenceEnd)
                        && booking.getEndDateTime().isAfter(occurrenceStart));
    }

    private RecurringBooking getOwned(UUID userId, UUID id) {
        RecurringBooking recurringBooking = getRequired(id);
        if (!recurringBooking.getUserId().equals(userId)) {
            throw new UnauthorizedException("You are not authorised to manage this recurring booking");
        }
        return recurringBooking;
    }

    private RecurringBooking getOwnedByFieldOwner(UUID ownerId, UUID id) {
        RecurringBooking recurringBooking = getRequired(id);
        if (recurringBooking.getSubField() == null || !ownerId.equals(recurringBooking.getSubField().getOwnerId())) {
            throw new UnauthorizedException("You are not authorised to manage this recurring booking");
        }
        return recurringBooking;
    }

    private RecurringBooking getRequired(UUID id) {
        return recurringBookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Recurring booking not found with id: " + id, "RECURRING_BOOKING_NOT_FOUND"));
    }

    private void validateRule(LocalTime startTime, LocalTime endTime, LocalDate startDate, LocalDate endDate,
                              Integer intervalDays) {
        if (startTime.equals(endTime)) {
            throw new BadRequestException("End time must be different from start time");
        }
        if (!endDate.isAfter(startDate)) {
            throw new BadRequestException("End date must be after start date");
        }
        validateRecurringEndDateLimit(startDate, endDate);
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
            throw new ForbiddenException(ELIGIBILITY_MESSAGE, ELIGIBILITY_CODE);
        }
    }

    private void validateNoRecurringConflicts(UUID userId, UUID subFieldId, LocalTime startTime, LocalTime endTime,
                                              LocalDate startDate, LocalDate endDate, int intervalDays, UUID excludeId) {
        LocalDate candidateStartDate = startDate.minusDays(1);
        boolean userOverlap = recurringBookingRepository.overlapsAnyGeneratedOccurrence(
                recurringBookingRepository.findUserOverlapCandidates(
                        userId, candidateStartDate, endDate, RecurringBookingStatus.ACTIVE, excludeId),
                startTime, endTime, startDate, endDate, intervalDays);
        boolean subFieldOverlap = recurringBookingRepository.overlapsAnyGeneratedOccurrence(
                recurringBookingRepository.findSubFieldOverlapCandidates(
                        subFieldId, candidateStartDate, endDate, RecurringBookingStatus.ACTIVE, excludeId),
                startTime, endTime, startDate, endDate, intervalDays);
        if (userOverlap || subFieldOverlap) {
            throw new ConflictException("Recurring booking overlaps an existing recurring booking.", "RECURRING_BOOKING_CONFLICT");
        }
    }

    private void validateEndDateOnlyUpdate(RecurringBooking recurringBooking, UpdateRecurringBookingRequest request) {
        if (!recurringBooking.getSubFieldId().equals(request.getSubFieldId())
                || !recurringBooking.getStartTime().equals(request.getStartTime())
                || !recurringBooking.getEndTime().equals(request.getEndTime())
                || !recurringBooking.getStartDate().equals(request.getStartDate())
                || !recurringBooking.getIntervalDays().equals(request.getIntervalDays())) {
            throw new BadRequestException(IMMUTABLE_UPDATE_MESSAGE, "BOOKING_CANNOT_MODIFY");
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
        validateRecurringEndDateLimit(startDate, endDate);
    }

    private void validateRecurringEndDateLimit(LocalDate startDate, LocalDate endDate) {
        LocalDate latestEndDate = startDate.plusYears(MAX_RECURRING_BOOKING_YEARS);
        if (endDate.isAfter(latestEndDate)) {
            throw new BadRequestException(
                    "Recurring booking end date cannot be more than 1 year after the start date",
                    "RECURRING_BOOKING_END_DATE_OUT_OF_RANGE");
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

    private void cancelLatestConfirmedBookingByOwner(UUID ownerId, RecurringBooking recurringBooking) {
        bookingRepository.findFirstBySourceRecurringBookingIdOrderByStartDateTimeDesc(recurringBooking.getId())
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED)
                .ifPresent(booking -> bookingService.cancelBookingByManager(ownerId, "OWNER", CancelBookingRequest.builder()
                        .bookingId(booking.getId())
                        .reason("Recurring booking cancelled by field owner")
                        .build()));
    }

    private void validateCanResume(RecurringBooking recurringBooking) {
        if (recurringBooking.getStatus() == RecurringBookingStatus.COMPLETED) {
            throw new BadRequestException("Completed recurring bookings cannot be resumed", "RECURRING_BOOKING_ALREADY_ACTIVE");
        }
        bookingModerationService.ensureCanBook(recurringBooking.getUserId(), recurringBooking.getFieldId());
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
        for (CreateBookingRequest occurrence : occurrences) {
            try {
                bookingService.validateRecurringOccurrence(userId, occurrence, recurringBookingId);
            } catch (BadRequestException ex) {
                if (SUBFIELD_CLOSED_CODE.equals(ex.getCode())) {
                    LocalDate closedDate = occurrence.getBookingDate();
                    String message = "Sân sẽ đóng vào ngày "
                            + closedDate.format(DISPLAY_DATE_FORMATTER)
                            + ".";
                    throw new BadRequestException(
                            message,
                            RECURRING_SUBFIELD_CLOSED_ON_DATE_CODE,
                            message);
                }
                throw ex;
            }
        }
    }

    private List<CreateBookingRequest> generateOccurrences(UUID subFieldId, LocalTime startTime, LocalTime endTime,
                                                           LocalDate startDate, LocalDate endDate, int intervalDays) {
        int durationMinutes = durationMinutes(startTime, endTime);
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

    private int durationMinutes(LocalTime startTime, LocalTime endTime) {
        int minutes = (int) Duration.between(startTime, endTime).toMinutes();
        return minutes > 0 ? minutes : minutes + (24 * 60);
    }

    private List<LocalDate> generateOccurrenceDates(LocalDate startDate, LocalDate endDate, int intervalDays) {
        return startDate.datesUntil(endDate.plusDays(1), java.time.Period.ofDays(intervalDays)).toList();
    }

    private LocalDateTime nextMatchAt(RecurringBooking recurringBooking) {
        if (recurringBooking.getStatus() != RecurringBookingStatus.ACTIVE) {
            return null;
        }
        LocalDate matchDate = nextOccurrenceOnOrAfter(recurringBooking, LocalDate.now());
        if (matchDate.isAfter(recurringBooking.getEndDate())) {
            return null;
        }
        return LocalDateTime.of(matchDate, recurringBooking.getStartTime());
    }

    private List<LocalDate> occurrenceDatesInGenerationWindow(RecurringBooking recurringBooking, LocalDate windowStart) {
        LocalDate firstDate = nextOccurrenceOnOrAfter(recurringBooking, windowStart);
        LocalDate windowEnd = windowStart.plusDays(generationWindowDays);
        LocalDate boundedEnd = windowEnd.isBefore(recurringBooking.getEndDate()) ? windowEnd : recurringBooking.getEndDate();
        if (firstDate.isAfter(boundedEnd)) {
            return List.of();
        }
        return firstDate.datesUntil(boundedEnd.plusDays(1), java.time.Period.ofDays(recurringBooking.getIntervalDays())).toList();
    }

    private record ResumeWindowResult(
            List<LocalDate> generatedDates,
            List<LocalDate> occupiedDates,
            boolean allOccurrencesOccupied) {
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
