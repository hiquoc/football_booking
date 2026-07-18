package com.project.booking.service.impl;

import com.project.booking.cache.AvailabilityCacheService;
import com.project.booking.community.service.CommunityPostMaintenanceService;
import com.project.booking.config.BookingDatabaseConstraints;
import com.project.booking.dto.request.CancelBookingRequest;
import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.response.AvailabilityResponse;
import com.project.booking.dto.response.BookingResponse;
import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.dto.response.TimePriceRuleDto;
import com.project.booking.dto.response.UnavailableSlotResponse;
import com.project.booking.entity.Booking;
import com.project.booking.exception.BookingConflictException;
import com.project.booking.exception.BookingNotCancellableException;
import com.project.booking.exception.BookingNotFoundException;
import com.project.booking.kafka.BookingBalanceEventPublisher;
import com.project.booking.kafka.BookingNotificationEventPublisher;
import com.project.booking.kafka.BookingTrustEventPublisher;
import com.project.booking.mapper.BookingMapper;
import com.project.booking.moderation.service.BookingModerationService;
import com.project.booking.payment.BookingPaymentStrategyFactory;
import com.project.booking.pricing.PricingStrategy;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.FieldClosureProjectionRepository;
import com.project.booking.repository.MatchResultRepository;
import com.project.booking.repository.RecurringBookingRepository;
import com.project.booking.repository.UserReplicaRepository;
import com.project.booking.service.BookingConfigService;
import com.project.booking.service.BookingService;
import com.project.booking.service.ResolvedOperatingHours;
import com.project.booking.service.SubFieldProjectionService;
import com.project.booking.util.BookingCodeGenerator;
import com.project.common.cache.CacheKeys;
import com.project.common.cache.CacheNames;
import com.project.common.dto.PageResponse;
import com.project.common.enums.BookingCancelledBy;
import com.project.common.enums.BookingStatus;
import com.project.common.enums.PaymentMethod;
import com.project.common.enums.RecurringBookingStatus;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
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
    private static final int MAX_BOOKING_MINUTES = 300;
    private static final int START_TIME_INTERVAL_MINUTES = 30;
    private static final LocalTime END_OF_DAY_BOOKING_TIME = LocalTime.of(23, 59);
    private static final Collection<BookingStatus> RESERVING_STATUSES = Arrays.asList(BookingStatus.PENDING,
            BookingStatus.CONFIRMED);
    private static final Collection<BookingStatus> CANCELLABLE_STATUSES = Arrays.asList(BookingStatus.PENDING,
            BookingStatus.CONFIRMED);
    private static final String BOOKING_CONFLICT_MESSAGE = "The selected time slot is no longer available.";
    private static final String EXCLUSION_VIOLATION_SQL_STATE = "23P01";
    private static final String PAYMENT_TIMEOUT_REASON = "Payment timeout";
    private static final String BOOKING_CANCEL_REFUND_REASON = "BOOKING_CANCEL_REFUND";

    private final BookingRepository bookingRepository;
    private final SubFieldProjectionService subFieldProjectionService;
    private final FieldClosureProjectionRepository fieldClosureProjectionRepository;
    private final PricingStrategy pricingStrategy;
    private final BookingMapper bookingMapper;
    private final BookingNotificationEventPublisher bookingNotificationEventPublisher;
    private final BookingConfigService bookingConfigService;
    private final BookingPaymentStrategyFactory paymentStrategyFactory;
    private final BookingBalanceEventPublisher bookingBalanceEventPublisher;
    private final AvailabilityCacheService availabilityCacheService;
    private final RecurringBookingRepository recurringBookingRepository;
    private final CommunityPostMaintenanceService communityPostMaintenanceService;
    private final UserReplicaRepository userReplicaRepository;
    private final BookingTrustEventPublisher bookingTrustEventPublisher;
    private final BookingModerationService bookingModerationService;
    private final MatchResultRepository matchResultRepository;

    @Value("${booking.payment-timeout-minutes:35}")
    private int paymentTimeoutMinutes = 35;

    @Override
    @Transactional
    public BookingResponse createBooking(UUID userId, CreateBookingRequest request) {
        return createBooking(userId, request, null);
    }

    @Override
    @Transactional
    public BookingResponse createRecurringOccurrence(UUID userId, UUID recurringBookingId, CreateBookingRequest request) {
        if (bookingRepository.existsBySourceRecurringBookingIdAndBookingDate(recurringBookingId, request.getBookingDate())) {
            throw new BookingConflictException(BOOKING_CONFLICT_MESSAGE);
        }
        return createBooking(userId, request, recurringBookingId);
    }

    private BookingResponse createBooking(UUID userId, CreateBookingRequest request, UUID sourceRecurringBookingId) {
        SubFieldResponse subField = subFieldProjectionService.getRequiredSubField(request.getSubFieldId());
        bookingModerationService.ensureCanBook(userId, subField.getFieldId());
        request.setEndTime(calculateEndTime(request.getStartTime(), request.getDurationMinutes()));

        validateBooking(subField, request, sourceRecurringBookingId);

        long bookingPrice = resolveBookingPrice(userId);
        PaymentMethod paymentMethod = request.getPaymentMethod() == null ? PaymentMethod.STRIPE : request.getPaymentMethod();
        BigDecimal subFieldPrice = pricingStrategy.calculate(subField, request);
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
                .totalAmount(subFieldPrice)
                .subFieldPrice(subFieldPrice)
                .bookingPrice(bookingPrice)
                .platformBookingFee(bookingPrice)
                .paymentMethod(paymentMethod)
                .status(BookingStatus.PENDING)
                .note(request.getNote())
                .sourceRecurringBookingId(sourceRecurringBookingId)
                .build();

        Booking saved;
        try {
            saved = bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            /// them check neu chinh nguoi dung da book san nay de tra ve loi Ban da dat san thanh cong thay vi tra loi
            /// Booking existing = bookingRepository.findByClientIdAndSubFieldIdAndBookingDateAndTime(...);
            ///
            if (isBookingOverlapConstraintViolation(ex)) {
                throw new BookingConflictException(BOOKING_CONFLICT_MESSAGE);
            }
            throw ex;
        }
        log.info("Booking created: code={}, clientId={}, subFieldId={}", saved.getBookingCode(), userId, subField.getId());
        availabilityCacheService.evict(saved.getSubFieldId(), saved.getBookingDate());
        paymentStrategyFactory.get(saved.getPaymentMethod()).onBookingCreated(saved, subField);
        return bookingMapper.toResponse(saved, subField);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID userId, CancelBookingRequest request) {
        int updatedRows = bookingRepository.cancelClientBooking(
                request.getBookingId(), userId, CANCELLABLE_STATUSES, BookingStatus.CANCELLED,
                request.getReason(), LocalDateTime.now(), BookingCancelledBy.CLIENT);

        if (updatedRows == 0) {
            Booking current = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new BookingNotFoundException(request.getBookingId()));
            if (!current.getClientId().equals(userId)) {
                throw new UnauthorizedException("You are not authorised to cancel this booking");
            }
            throw new BookingNotCancellableException(current.getId(), current.getStatus().name());
        }

        Booking cancelled = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(request.getBookingId()));
        log.info("Booking cancelled by client: id={}, clientId={}", cancelled.getId(), userId);
        publishRefundIfEligible(cancelled);
        availabilityCacheService.evict(cancelled.getSubFieldId(), cancelled.getBookingDate());
        communityPostMaintenanceService.cancelOpenPostForBooking(cancelled.getId());
        bookingNotificationEventPublisher.publishBookingCancelled(cancelled, null);
        return bookingMapper.toResponse(cancelled);
    }

    @Override
    @Transactional
    public BookingResponse cancelBookingByOwner(UUID ownerId, CancelBookingRequest request) {
        int updatedRows = bookingRepository.cancelOwnerBooking(
                request.getBookingId(), ownerId, CANCELLABLE_STATUSES, BookingStatus.CANCELLED,
                request.getReason(), LocalDateTime.now(), BookingCancelledBy.OWNER);

        if (updatedRows == 0) {
            Booking current = bookingRepository.findById(request.getBookingId())
                    .orElseThrow(() -> new BookingNotFoundException(request.getBookingId()));
            if (!current.getOwnerId().equals(ownerId)) {
                throw new UnauthorizedException("You are not authorised to cancel this booking");
            }
            throw new BookingNotCancellableException(current.getId(), current.getStatus().name());
        }

        Booking cancelled = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(request.getBookingId()));
        log.info("Booking cancelled by owner: id={}, ownerId={}", cancelled.getId(), ownerId);
        publishRefundIfEligible(cancelled);
        availabilityCacheService.evict(cancelled.getSubFieldId(), cancelled.getBookingDate());
        communityPostMaintenanceService.cancelOpenPostForBooking(cancelled.getId());
        bookingNotificationEventPublisher.publishBookingCancelled(cancelled, null);
        return bookingMapper.toResponse(cancelled);
    }

    @Override
    @Transactional
    public int expirePendingBookings() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresBefore = now.minusMinutes(paymentTimeoutMinutes);
        int expiredCount = bookingRepository.expirePendingBookings(
                BookingStatus.PENDING, BookingStatus.EXPIRED, expiresBefore,
                PAYMENT_TIMEOUT_REASON, now, BookingCancelledBy.SYSTEM);
        if (expiredCount > 0) {
            availabilityCacheService.evictAll();
            log.info("Expired {} pending bookings older than {}", expiredCount, expiresBefore);
        }
        return expiredCount;
    }

    @Override
    @Transactional
    public int completeFinishedBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> finishedBookings = bookingRepository.findFinishedConfirmedBookings(
                BookingStatus.CONFIRMED, now.toLocalDate(), now.toLocalTime());
        finishedBookings.forEach(booking -> booking.setStatus(BookingStatus.COMPLETED));
        int completedCount = finishedBookings.size();
        if (completedCount > 0) {
            availabilityCacheService.evictAll();
            bookingRepository.saveAll(finishedBookings);
            finishedBookings.forEach(bookingTrustEventPublisher::publishBookingCompleted);
            log.info("Completed {} confirmed bookings ending on or before {}", completedCount, now);
        }
        return completedCount;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getMyBookings(UUID userId, Pageable pageable) {
        return PageResponse.from(bookingRepository.findByClientId(userId, pageable).map(bookingMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getOwnerBookings(UUID ownerId, LocalDate bookingDate, UUID subFieldId, BookingStatus status, Pageable pageable) {
        var page = bookingRepository.findOwnerBookings(ownerId, bookingDate, subFieldId, status, pageable);
        var responses = page.map(bookingMapper::toResponse);
        var resultByBookingId = matchResultRepository.findByBookingIdIn(
                        responses.getContent().stream().map(BookingResponse::getId).toList())
                .stream()
                .collect(Collectors.toMap(result -> result.getBookingId(), bookingMapper::toMatchResultResponse));
        responses.getContent().forEach(response -> response.setMatchResult(resultByBookingId.get(response.getId())));
        return PageResponse.from(responses);
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
    @Cacheable(cacheNames = CacheNames.AVAILABILITY, key = CacheKeys.AVAILABILITY, sync = true)
    public AvailabilityResponse getAvailability(UUID subFieldId, LocalDate date) {
        validateBookingDateNotPast(date);
        SubFieldResponse subField = subFieldProjectionService.getRequiredSubField(subFieldId);
        ResolvedOperatingHours hours = subFieldProjectionService.resolveOperatingHours(subFieldId, date.getDayOfWeek());

        List<Booking> existingBookings = bookingRepository
                .findBySubFieldIdAndBookingDateAndStatusInOrderByStartTimeAsc(subFieldId, date, RESERVING_STATUSES);
        List<UnavailableSlotResponse> unavailableSlots = existingBookings.stream()
                .map(booking -> UnavailableSlotResponse.builder()
                        .startTime(booking.getStartTime())
                        .endTime(booking.getEndTime())
                        .build())
                .collect(Collectors.toList());

        if (Boolean.TRUE.equals(subField.getHasRecurring())) {
            recurringBookingRepository.findActiveReservationsForDate(
                    subFieldId, date.getDayOfWeek(), date, RecurringBookingStatus.ACTIVE)
                    .stream()
                    .map(recurring -> UnavailableSlotResponse.builder()
                            .startTime(recurring.getStartTime())
                            .endTime(recurring.getEndTime())
                            .build())
                    .forEach(unavailableSlots::add);
        }

        return AvailabilityResponse.builder()
                .openTime(hours.closed() ? null : hours.openTime())
                .closeTime(hours.closed() ? null : hours.closeTime())
                .unavailableSlots(unavailableSlots)
                .build();
    }

    private void publishRefundIfEligible(Booking booking) {
        var config = bookingConfigService.getConfig();
        long refundAmount = booking.getBookingPrice() == null || booking.getBookingPrice() == 0L
                ? (booking.getPlatformBookingFee() == null ? 0L : booking.getPlatformBookingFee())
                : booking.getBookingPrice();
        if (!Boolean.TRUE.equals(config.getRefundEnabled()) || refundAmount <= 0) {
            return;
        }
        LocalDateTime refundDeadline = LocalDateTime.of(booking.getBookingDate(), booking.getStartTime())
                .minusHours(config.getRefundBeforeHours());
        if (LocalDateTime.now().isBefore(refundDeadline)) {
            bookingBalanceEventPublisher.publishRefundRequested(booking, refundAmount, BOOKING_CANCEL_REFUND_REASON);
        }
    }

    private long resolveBookingPrice(UUID userId) {
        int completedBookingCount = userReplicaRepository.findById(userId)
                .map(replica -> replica.getCompletedBookingCount() == null ? 0 : replica.getCompletedBookingCount())
                .orElse(0);

        var config = bookingConfigService.getConfig();
        return completedBookingCount == 0 ? config.getFirstBookingFee() : config.getNotFirstBookingFee();
    }

    private void validateBooking(SubFieldResponse subField, CreateBookingRequest request, UUID sourceRecurringBookingId) {
        validateSubFieldActive(subField);
        validateBookingDateNotPast(request.getBookingDate());
        validateBookingStartNotPast(request.getBookingDate(), request.getStartTime());
        validateClosureDate(subField.getId(), request.getBookingDate());
        ResolvedOperatingHours hours = subFieldProjectionService.resolveOperatingHours(
                subField.getId(), request.getBookingDate().getDayOfWeek());
        validateWithinOperatingHours(request.getStartTime(), request.getEndTime(), hours);
        validateDuration(request.getDurationMinutes(), subField);
        validateStartTimeAlignment(request.getStartTime());
        validateNoConflict(request.getSubFieldId(), request.getBookingDate(), request.getStartTime(), request.getEndTime(), sourceRecurringBookingId);
    }

    private void validateSubFieldActive(SubFieldResponse subField) {
        if (Boolean.FALSE.equals(subField.getActive()) || !"ACTIVE".equalsIgnoreCase(subField.getStatus())) {
            throw new BadRequestException("SubField '" + subField.getName() + "' is not currently available for booking");
        }
    }

    private void validateClosureDate(UUID subFieldId, LocalDate bookingDate) {
        boolean closed = fieldClosureProjectionRepository
                .existsBySubFieldIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(subFieldId, bookingDate, bookingDate);
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

    private void validateWithinOperatingHours(LocalTime startTime, LocalTime endTime, ResolvedOperatingHours hours) {
        if (hours.closed()) {
            throw new BadRequestException("SubField is closed on the selected booking date");
        }
        LocalTime openTime = hours.openTime();
        LocalTime closeTime = hours.closeTime();
        if (openTime == null || closeTime == null || startTime.isBefore(openTime) || endTime.isAfter(closeTime)) {
            throw new BadRequestException("Booking time must be within operating hours: " + openTime + " - " + closeTime);
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
            throw new BadRequestException("Minimum booking duration for this sub-field is " + minMinutes + " minutes");
        }
        if (minutes > maxMinutes) {
            throw new BadRequestException("Maximum booking duration for this sub-field is " + maxMinutes + " minutes");
        }
    }

    private void validateStartTimeAlignment(LocalTime startTime) {
        int minute = startTime.getMinute();
        if (minute % START_TIME_INTERVAL_MINUTES != 0 || startTime.getSecond() != 0 || startTime.getNano() != 0) {
            throw new BadRequestException("Booking start time must align with the " + START_TIME_INTERVAL_MINUTES + "-minute interval");
        }
    }

    private int resolveMinimumBookingMinutes(SubFieldResponse subField) {
        return subField.getMinimumBookingDurationMinutes() != null ? subField.getMinimumBookingDurationMinutes() : MIN_BOOKING_MINUTES;
    }

    private int resolveMaximumBookingMinutes(SubFieldResponse subField) {
        return subField.getMaximumBookingDurationMinutes() != null ? subField.getMaximumBookingDurationMinutes() : MAX_BOOKING_MINUTES;
    }

    private void validateNoConflict(UUID subFieldId, LocalDate bookingDate, LocalTime startTime, LocalTime endTime, UUID sourceRecurringBookingId) {
        boolean isConflict = bookingRepository.existsConflictingBookings(subFieldId, bookingDate, startTime, endTime, RESERVING_STATUSES);
        boolean recurringConflict = !recurringBookingRepository.findActiveConflictsForDate(
                subFieldId,
                bookingDate.getDayOfWeek(),
                bookingDate,
                startTime,
                endTime,
                RecurringBookingStatus.ACTIVE,
                sourceRecurringBookingId).isEmpty();
        if (isConflict || recurringConflict) {
            throw new BookingConflictException(BOOKING_CONFLICT_MESSAGE);
        }
    }

    private boolean isBookingOverlapConstraintViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains(BookingDatabaseConstraints.ACTIVE_BOOKING_OVERLAP_CONSTRAINT)) {
                return true;
            }
            if (current instanceof SQLException sqlException && EXCLUSION_VIOLATION_SQL_STATE.equals(sqlException.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
