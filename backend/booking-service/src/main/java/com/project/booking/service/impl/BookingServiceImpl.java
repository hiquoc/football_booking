package com.project.booking.service.impl;

import com.project.booking.dto.request.CancelBookingRequest;
import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.response.AvailabilityResponse;
import com.project.booking.dto.response.BookingResponse;
import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.dto.response.TimePriceRuleDto;
import com.project.booking.dto.response.UnavailableSlotResponse;
import com.project.booking.entity.Booking;
import com.project.booking.config.BookingDatabaseConstraintInitializer;
import com.project.booking.exception.BookingConflictException;
import com.project.booking.exception.BookingNotCancellableException;
import com.project.booking.exception.BookingNotFoundException;
import com.project.booking.mapper.BookingMapper;
import com.project.booking.pricing.PricingStrategy;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.FieldClosureProjectionRepository;
import com.project.booking.service.BookingService;
import com.project.booking.service.ResolvedOperatingHours;
import com.project.booking.service.SubFieldProjectionService;
import com.project.booking.util.BookingCodeGenerator;
import com.project.common.enums.BookingCancelledBy;
import com.project.common.enums.BookingStatus;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final int MIN_BOOKING_MINUTES = 60;
    private static final int MAX_BOOKING_MINUTES = 300; // 5 hours
    private static final int START_TIME_INTERVAL_MINUTES = 30;
    private static final LocalTime END_OF_DAY_BOOKING_TIME = LocalTime.of(23, 59);
    private static final Collection<BookingStatus> RESERVING_STATUSES = Arrays.asList(BookingStatus.PENDING,
            BookingStatus.CONFIRMED);
    private static final Collection<BookingStatus> CANCELLABLE_STATUSES = Arrays.asList(BookingStatus.PENDING,
            BookingStatus.CONFIRMED);
    private static final String BOOKING_CONFLICT_MESSAGE = "The selected time slot is no longer available.";
    private static final String EXCLUSION_VIOLATION_SQL_STATE = "23P01";
    private static final String PAYMENT_TIMEOUT_REASON = "Payment timeout";

    private final BookingRepository bookingRepository;
    private final SubFieldProjectionService subFieldProjectionService;
    private final FieldClosureProjectionRepository fieldClosureProjectionRepository;
    private final PricingStrategy pricingStrategy;
    private final BookingMapper bookingMapper;

    @Value("${booking.payment-timeout-minutes:15}")
    private int paymentTimeoutMinutes = 15;

    @Override
    @Transactional
    public BookingResponse createBooking(UUID userId, CreateBookingRequest request) {
        // VALIDATION: Fetch sub-field — throws if not found
        SubFieldResponse subField = subFieldProjectionService.getRequiredSubField(request.getSubFieldId());
        request.setEndTime(calculateEndTime(request.getStartTime(), request.getDurationMinutes()));

        validateBooking(subField, request);

        // VALIDATION: Calculate price
        BigDecimal totalAmount = pricingStrategy.calculate(subField, request);
        Booking booking = Booking.builder()
                .bookingCode(BookingCodeGenerator.generate())
                .clientId(userId)
                .subFieldId(subField.getId())
                .ownerId(subField.getOwnerId())
                .bookingDate(request.getBookingDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .durationMinutes(request.getDurationMinutes())
                .pricePerHour(resolveStartPrice(subField, request.getStartTime()))
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING)
                .note(request.getNote())
                .build();

        Booking saved;
        try {
            saved = bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            if (isBookingOverlapConstraintViolation(ex)) {
                throw new BookingConflictException(BOOKING_CONFLICT_MESSAGE);
            }
            throw ex;
        }
        log.info("Booking created: code={}, clientId={}, subFieldId={}",
                saved.getBookingCode(), userId, subField.getId());

        return bookingMapper.toResponse(saved, subField);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID userId, CancelBookingRequest request) {
        int updatedRows = bookingRepository.cancelClientBooking(
                request.getBookingId(),
                userId,
                CANCELLABLE_STATUSES,
                BookingStatus.CANCELLED,
                request.getReason(),
                LocalDateTime.now(),
                BookingCancelledBy.CLIENT);

        if (updatedRows == 0) {
            Booking current = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new BookingNotFoundException(request.getBookingId()));

            if (!current.getClientId().equals(userId)) {
                throw new UnauthorizedException(
                        "You are not authorised to cancel this booking");
            }

            throw new BookingNotCancellableException(
                    current.getId(),
                    current.getStatus().name());
        }

        Booking cancelled = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(request.getBookingId()));

        log.info("Booking cancelled by client: id={}, clientId={}",
                cancelled.getId(),
                userId);

        return bookingMapper.toResponse(cancelled);
    }

    @Override
    @Transactional
    public BookingResponse cancelBookingByOwner(UUID ownerId, CancelBookingRequest request) {
        int updatedRows = bookingRepository.cancelOwnerBooking(
                request.getBookingId(),
                ownerId,
                CANCELLABLE_STATUSES,
                BookingStatus.CANCELLED,
                request.getReason(),
                LocalDateTime.now(),
                BookingCancelledBy.OWNER);

        if (updatedRows == 0) {
            Booking current = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new BookingNotFoundException(request.getBookingId()));

            if (!current.getOwnerId().equals(ownerId)) {
                throw new UnauthorizedException(
                        "You are not authorised to cancel this booking");
            }

            throw new BookingNotCancellableException(
                    current.getId(),
                    current.getStatus().name());
        }

        Booking cancelled = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(request.getBookingId()));

        log.info("Booking cancelled by owner: id={}, ownerId={}",
                cancelled.getId(),
                ownerId);

        return bookingMapper.toResponse(cancelled);
    }

    @Override
    @Transactional
    public BookingResponse confirmMockPayment(UUID userId, UUID bookingId) {

        int updatedRows = bookingRepository.confirmPendingBooking(
                bookingId,
                userId,
                BookingStatus.PENDING,
                BookingStatus.CONFIRMED);

        if (updatedRows == 0) {
            Booking current = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new BookingNotFoundException(bookingId));

            if (!current.getClientId().equals(userId)) {
                throw new UnauthorizedException(
                        "You are not authorised to pay for this booking");
            }

            throw new BadRequestException(
                    "Only pending bookings can be confirmed. Current status: "
                            + current.getStatus());
        }

        Booking confirmed = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        log.info("Mock payment confirmed booking: id={}, clientId={}",
                confirmed.getId(), userId);

        return bookingMapper.toResponse(confirmed);
    }

    @Override
    @Transactional
    public int expirePendingBookings() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresBefore = now.minusMinutes(paymentTimeoutMinutes);
        int expiredCount = bookingRepository.expirePendingBookings(
                BookingStatus.PENDING,
                BookingStatus.EXPIRED,
                expiresBefore,
                PAYMENT_TIMEOUT_REASON,
                now,
                BookingCancelledBy.SYSTEM);
        if (expiredCount > 0) {
            log.info("Expired {} pending bookings older than {}", expiredCount, expiresBefore);
        }
        return expiredCount;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(UUID userId) {
        return bookingRepository.findByClientId(userId)
                .stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getOwnerBookings(UUID ownerId) {
        return bookingRepository.findByOwnerId(ownerId)
                .stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID bookingId, UUID userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (!booking.getClientId().equals(userId)) {
            throw new UnauthorizedException("You are not authorised to view this booking");
        }

        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailability(UUID subFieldId, LocalDate date) {
        validateBookingDateNotPast(date);
        subFieldProjectionService.getRequiredSubField(subFieldId);
        ResolvedOperatingHours hours = subFieldProjectionService.resolveOperatingHours(subFieldId, date.getDayOfWeek());

        List<Booking> existingBookings = bookingRepository
                .findBySubFieldIdAndBookingDateAndStatusInOrderByStartTimeAsc(subFieldId, date, RESERVING_STATUSES);

        List<UnavailableSlotResponse> unavailableSlots = existingBookings.stream()
                .map(booking -> UnavailableSlotResponse.builder()
                        .startTime(booking.getStartTime())
                        .endTime(booking.getEndTime())
                        .build())
                .collect(Collectors.toList());

        return AvailabilityResponse.builder()
                .openTime(hours.closed() ? null : hours.openTime())
                .closeTime(hours.closed() ? null : hours.closeTime())
                .unavailableSlots(unavailableSlots)
                .build();
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    private void validateBooking(SubFieldResponse subField, CreateBookingRequest request) {
        // VALIDATION 1: Sub-field must be ACTIVE
        validateSubFieldActive(subField);

        // VALIDATION 2: Booking date must not be in the past
        validateBookingDateNotPast(request.getBookingDate());
        validateBookingStartNotPast(request.getBookingDate(), request.getStartTime());

        // VALIDATION 3: Future booking maintenance closures
        validateClosureDate(subField.getId(), request.getBookingDate());

        // VALIDATION 4: Times must be within operating hours
        ResolvedOperatingHours hours = subFieldProjectionService.resolveOperatingHours(
                subField.getId(),
                request.getBookingDate().getDayOfWeek());
        validateWithinOperatingHours(request.getStartTime(), request.getEndTime(), hours);

        // VALIDATION 5: Duration must be within allowed bounds
        validateDuration(request.getDurationMinutes(), subField);

        // VALIDATION 6: Booking start time must align to the fixed 30-minute boundary
        validateStartTimeAlignment(request.getStartTime());

        // VALIDATION 7: No overlapping confirmed/pending bookings
        validateNoConflict(request.getSubFieldId(), request.getBookingDate(),
                request.getStartTime(), request.getEndTime());

    }

    private void validateSubFieldActive(SubFieldResponse subField) {
        if (Boolean.FALSE.equals(subField.getActive()) || !"ACTIVE".equalsIgnoreCase(subField.getStatus())) {
            throw new BadRequestException(
                    "SubField '" + subField.getName() + "' is not currently available for booking");
        }
    }


    private void validateClosureDate(UUID subFieldId, LocalDate bookingDate) {
        boolean closed = fieldClosureProjectionRepository
                .existsBySubFieldIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        subFieldId,
                        bookingDate,
                        bookingDate);
        if (closed) {
            throw new BadRequestException("SubField is closed on the selected booking date");
        }
    }

    private void validateBookingDateNotPast(LocalDate bookingDate) {
        if (bookingDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Booking date cannot be in the past");
        }
    }

    private void validateBookingStartNotPast(LocalDate bookingDate, LocalTime startTime) {
        LocalDateTime bookingStart = LocalDateTime.of(bookingDate, startTime);

        if (!bookingStart.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Booking start time must be in the future");
        }
    }

    private void validateWithinOperatingHours(LocalTime startTime, LocalTime endTime,
            ResolvedOperatingHours hours) {
        if (hours.closed()) {
            throw new BadRequestException("SubField is closed on the selected booking date");
        }

        LocalTime openTime = hours.openTime();
        LocalTime closeTime = hours.closeTime();

        if (openTime == null || closeTime == null || startTime.isBefore(openTime) || endTime.isAfter(closeTime)) {
            throw new BadRequestException(
                    "Booking time must be within operating hours: "
                            + openTime + " – " + closeTime);
        }
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("End time must be after start time");
        }
    }

    private LocalTime calculateEndTime(LocalTime startTime, int durationMinutes) {
        LocalTime calculatedEndTime = startTime.plusMinutes(durationMinutes);
        return LocalTime.MIDNIGHT.equals(calculatedEndTime) ? END_OF_DAY_BOOKING_TIME : calculatedEndTime;
    }

    private BigDecimal resolveStartPrice(SubFieldResponse subField, LocalTime startTime) {
        if (subField.getTimePriceRules() == null || subField.getTimePriceRules().isEmpty()) {
            throw new BadRequestException("Time price rules are not configured for this sub-field");
        }
        return subField.getTimePriceRules().stream()
                .filter(rule -> isWithinRule(startTime, rule))
                .findFirst()
                .map(TimePriceRuleDto::getHourlyPrice)
                .orElseThrow(() -> new BadRequestException("Time price rules do not cover the requested booking time"));
    }

    private boolean isWithinRule(LocalTime time, TimePriceRuleDto rule) {
        return !time.isBefore(rule.getStartTime()) && time.isBefore(rule.getEndTime());
    }

    private void validateDuration(int minutes, SubFieldResponse subField) {
        int minMinutes = resolveMinimumBookingMinutes(subField);
        int maxMinutes = resolveMaximumBookingMinutes(subField);
                
        if (minutes < minMinutes) {
            throw new BadRequestException(
                    "Minimum booking duration for this sub-field is " + minMinutes + " minutes");
        }
        if (minutes > maxMinutes) {
            throw new BadRequestException(
                    "Maximum booking duration for this sub-field is " + maxMinutes + " minutes");
        }
    }

    private void validateStartTimeAlignment(LocalTime startTime) {
        int minute = startTime.getMinute();
        if (minute % START_TIME_INTERVAL_MINUTES != 0 || startTime.getSecond() != 0 || startTime.getNano() != 0) {
            throw new BadRequestException(
                    "Booking start time must align with the " + START_TIME_INTERVAL_MINUTES + "-minute interval");
        }
    }

    private int resolveMinimumBookingMinutes(SubFieldResponse subField) {
        return subField.getMinimumBookingDurationMinutes() != null
                ? subField.getMinimumBookingDurationMinutes()
                : MIN_BOOKING_MINUTES;
    }

    private int resolveMaximumBookingMinutes(SubFieldResponse subField) {
        return subField.getMaximumBookingDurationMinutes() != null
                ? subField.getMaximumBookingDurationMinutes()
                : MAX_BOOKING_MINUTES;
    }

    private void validateNoConflict(UUID subFieldId, LocalDate bookingDate,
            LocalTime startTime, LocalTime endTime) {
        boolean isConflict = bookingRepository.existsConflictingBookings(
                subFieldId, bookingDate, startTime, endTime, RESERVING_STATUSES);
        if (isConflict) {
            throw new BookingConflictException(BOOKING_CONFLICT_MESSAGE);
        }
    }

    private boolean isBookingOverlapConstraintViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(
                    BookingDatabaseConstraintInitializer.ACTIVE_BOOKING_OVERLAP_CONSTRAINT)) {
                return true;
            }
            if (current instanceof SQLException sqlException
                    && EXCLUSION_VIOLATION_SQL_STATE.equals(sqlException.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
