package com.project.booking.service.impl;

import com.project.booking.cache.AvailabilityCacheService;
import com.project.booking.client.FieldManagementClient;
import com.project.booking.client.UserBalanceClient;
import com.project.booking.config.BookingDatabaseConstraints;
import com.project.booking.dto.request.CancelBookingRequest;
import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.request.UpdateReservationRequest;
import com.project.booking.dto.response.AvailabilityOperatingHoursResponse;
import com.project.booking.dto.response.AvailabilityResponse;
import com.project.booking.dto.response.BookingResponse;
import com.project.booking.dto.response.MatchResultResponse;
import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.dto.response.TimePriceRuleDto;
import com.project.booking.dto.response.UnavailableSlotResponse;
import com.project.booking.entity.Booking;
import com.project.booking.entity.BookingConfig;
import com.project.booking.entity.MatchResult;
import com.project.booking.entity.SubFieldClosureProjection;
import com.project.booking.entity.UserProjection;
import com.project.booking.exception.BookingConflictException;
import com.project.booking.exception.BookingNotCancellableException;
import com.project.booking.exception.BookingNotFoundException;
import com.project.booking.kafka.BookingBalanceEventPublisher;
import com.project.booking.kafka.BookingNotificationEventPublisher;
import com.project.booking.kafka.BookingTrustEventPublisher;
import com.project.booking.lock.BookingLockManager;
import com.project.booking.mapper.BookingMapper;
import com.project.booking.moderation.service.BookingModerationService;
import com.project.booking.pricing.PricingStrategy;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.BookingSubFieldProjectionRepository;
import com.project.booking.repository.FieldClosureProjectionRepository;
import com.project.booking.repository.MatchResultRepository;
import com.project.booking.repository.RecurringBookingRepository;
import com.project.booking.repository.UserProjectionRepository;
import com.project.booking.service.BookingConfigService;
import com.project.booking.service.BookingService;
import com.project.booking.service.PendingBookingReservationService;
import com.project.booking.service.ResolvedOperatingHours;
import com.project.booking.service.SubFieldProjectionService;
import com.project.booking.util.BookingCodeGenerator;
import com.project.common.cache.CacheKeys;
import com.project.common.cache.CacheNames;
import com.project.common.dto.balance.BalanceDeductionResponse;
import com.project.common.dto.balance.BalanceDeductionRequest;
import com.project.common.dto.PageResponse;
import com.project.common.enums.BookingCancelledBy;
import com.project.common.enums.BookingPaymentStatus;
import com.project.common.enums.BookingStatus;
import com.project.common.enums.BookingType;
import com.project.common.enums.PaymentMethod;
import com.project.common.enums.RecurringBookingStatus;
import com.project.common.enums.SportType;
import com.project.common.enums.SubFieldType;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final int MIN_BOOKING_MINUTES = 60;
    private static final int MAX_BOOKING_MINUTES = 300;
    private static final int START_TIME_INTERVAL_MINUTES = 30;
    private static final Collection<BookingStatus> RESERVING_STATUSES = Arrays.asList(BookingStatus.PENDING,
            BookingStatus.CONFIRMED);
    private static final Collection<BookingStatus> CANCELLABLE_STATUSES = Arrays.asList(BookingStatus.PENDING,
            BookingStatus.CONFIRMED);
    private static final String BOOKING_CONFLICT_MESSAGE = "The selected time slot is no longer available.";
    private static final String EXCLUSION_VIOLATION_SQL_STATE = "23P01";
    private static final String PAYMENT_TIMEOUT_REASON = "Payment timeout";
    private static final String INSUFFICIENT_BALANCE_REASON = "Insufficient account balance";
    private static final String BOOKING_PAYMENT_REFUND_REASON = "BOOKING_PAYMENT_REFUND";
    private static final String BOOKING_PAYMENT_REASON = "BOOKING_ACCOUNT_BALANCE_PAYMENT";
    private static final String SUBFIELD_CLOSED_CODE = "SUBFIELD_CLOSED";
    private static final String SUBFIELD_CLOSED_MESSAGE = "Sub-field is closed on the selected booking date";
    private static final int NORMAL_PAYMENT_TIMEOUT_MINUTES = 1;
    private static final int RECURRING_PAYMENT_TIMEOUT_MINUTES = 30;
    private static final int MAX_BOOKING_DAYS_IN_FUTURE = 30;
    private static final LocalTime END_OF_DAY_TIME = LocalTime.of(23, 59);

    private final BookingRepository bookingRepository;
    private final SubFieldProjectionService subFieldProjectionService;
    private final FieldClosureProjectionRepository fieldClosureProjectionRepository;
    private final PricingStrategy pricingStrategy;
    private final BookingMapper bookingMapper;
    private final BookingNotificationEventPublisher bookingNotificationEventPublisher;
    private final BookingConfigService bookingConfigService;
    private final BookingBalanceEventPublisher bookingBalanceEventPublisher;
    private final UserBalanceClient userBalanceClient;
    private final AvailabilityCacheService availabilityCacheService;
    private final RecurringBookingRepository recurringBookingRepository;
    private final UserProjectionRepository userProjectionRepository;
    private final BookingTrustEventPublisher bookingTrustEventPublisher;
    private final BookingModerationService bookingModerationService;
    private final MatchResultRepository matchResultRepository;
    private final PendingBookingReservationService pendingBookingReservationService;
    private final TransactionTemplate transactionTemplate;
    private final BookingLockManager bookingLockManager;
    private final FieldManagementClient fieldManagementClient;
    private final BookingSubFieldProjectionRepository bookingSubFieldProjectionRepository;

    @Value("${booking.payment-timeout-minutes:1}")
    private int paymentTimeoutMinutes = NORMAL_PAYMENT_TIMEOUT_MINUTES;

    @Value("${booking.recurring-payment-timeout-minutes:30}")
    private int recurringPaymentTimeoutMinutes = RECURRING_PAYMENT_TIMEOUT_MINUTES;

    @Value("${booking.balance-deduction-attempts:4}")
    private int balanceDeductionAttempts = 4;

    @Value("${booking.balance-deduction-retry-delay-ms:1250}")
    private long balanceDeductionRetryDelayMs = 1_250L;

    @Value("${booking.max-booking-days-in-future:30}")
    private int maxBookingDaysInFuture = MAX_BOOKING_DAYS_IN_FUTURE;

    @Override
    public BookingResponse createBooking(UUID userId, CreateBookingRequest request) {
        return createBooking(userId, request, null);
    }

    @Override
    public BookingResponse createReservation(UUID ownerId, CreateBookingRequest request) {
        normalizeRequestDateTimes(request);
        return bookingLockManager.executeWithLock(
                request.getSubFieldId(),
                request.getStartDateTime(),
                request.getEndDateTime(),
                () -> createReservationWithExistingValidation(ownerId, request));
    }

    @Override
    public BookingResponse updateReservation(UUID ownerId, UpdateReservationRequest request) {
        CreateBookingRequest bookingRequest = request.toCreateBookingRequest();
        normalizeRequestDateTimes(bookingRequest);
        return bookingLockManager.executeWithLock(
                bookingRequest.getSubFieldId(),
                bookingRequest.getStartDateTime(),
                bookingRequest.getEndDateTime(),
                () -> updateReservationWithExistingValidation(ownerId, request.getReservationId(), bookingRequest));
    }

    @Override
    @Transactional
    public BookingResponse cancelReservation(UUID ownerId, CancelBookingRequest request) {
        Booking current = getOwnedReservation(ownerId, request.getBookingId());
        if (!CANCELLABLE_STATUSES.contains(current.getStatus())) {
            throw new BookingNotCancellableException(current.getId(), current.getStatus().name());
        }
        current.setStatus(BookingStatus.CANCELLED);
        current.setCancellationReason(request.getReason());
        current.setCancelledAt(LocalDateTime.now());
        current.setCancelledBy(BookingCancelledBy.OWNER);
        current.setPaymentStatus(BookingPaymentStatus.NOT_REQUIRED);
        Booking cancelled = bookingRepository.save(current);
        log.info("Reservation cancelled: reservationId={}, ownerId={}, fieldId={}, subFieldId={}, bookingType={}",
                cancelled.getId(), ownerId, resolveFieldId(cancelled), cancelled.getSubFieldId(), cancelled.getBookingType());
        availabilityCacheService.evict(cancelled.getSubFieldId(), cancelled.getBookingDate());
        bookingNotificationEventPublisher.publishBookingCancelled(cancelled, null);
        return bookingMapper.toResponse(cancelled);
    }

    @Override
    public BookingResponse createRecurringOccurrence(UUID userId, UUID recurringBookingId, CreateBookingRequest request) {
        normalizeRequestDateTimes(request);
        if (bookingRepository.existsBySourceRecurringBookingIdAndStartDateTime(recurringBookingId, request.getStartDateTime())) {
            throw new BookingConflictException(BOOKING_CONFLICT_MESSAGE);
        }
        return createBooking(userId, request, recurringBookingId);
    }

    @Override
    public void validateRecurringOccurrence(UUID userId, CreateBookingRequest request, UUID recurringBookingId) {
        SubFieldResponse subField = subFieldProjectionService.getRequiredSubField(request.getSubFieldId());
        if (subField.getOwnerId().equals(userId)) {
            throw new BadRequestException("Owners cannot create normal bookings for their own fields. Create a reservation instead.");
        }
        bookingModerationService.ensureCanBook(userId, subField.getFieldId());
        normalizeRequestDateTimes(request);
        validateBooking(subField, request, recurringBookingId, false);
    }

    private BookingResponse createBooking(UUID userId, CreateBookingRequest request, UUID sourceRecurringBookingId) {
        normalizeRequestDateTimes(request);
        return bookingLockManager.executeWithLock(
                request.getSubFieldId(),
                request.getStartDateTime(),
                request.getEndDateTime(),
                () -> createBookingWithExistingValidation(userId, request, sourceRecurringBookingId));
    }

    private BookingResponse createBookingWithExistingValidation(UUID userId, CreateBookingRequest request, UUID sourceRecurringBookingId) {
        SubFieldResponse subField = subFieldProjectionService.getRequiredSubField(request.getSubFieldId());
        if (subField.getOwnerId().equals(userId)) {
            log.warn("Owner attempted normal booking on owned field: userId={}, ownerId={}, fieldId={}, subFieldId={}, bookingType={}",
                    userId, subField.getOwnerId(), subField.getFieldId(), subField.getId(), BookingType.NORMAL);
            throw new BadRequestException("Owners cannot create normal bookings for their own fields. Create a reservation instead.");
        }
        bookingModerationService.ensureCanBook(userId, subField.getFieldId());

        validateBooking(subField, request, sourceRecurringBookingId);

        boolean recurringOccurrence = sourceRecurringBookingId != null;
        if (!recurringOccurrence) {
            ensureNoPendingPayment(userId);
        }
        long bookingPrice = resolveBookingPrice(userId);
        PaymentMethod paymentMethod = PaymentMethod.ACCOUNT_BALANCE;
        BigDecimal subFieldPrice = pricingStrategy.calculate(subField, request);
        LocalDateTime paymentExpiresAt = LocalDateTime.now().plusMinutes(
                recurringOccurrence ? recurringPaymentTimeoutMinutes : paymentTimeoutMinutes);
        Booking booking = Booking.builder()
                .bookingCode(BookingCodeGenerator.generate())
                .clientId(userId)
                .subFieldId(subField.getId())
                .ownerId(subField.getOwnerId())
                .bookingDate(request.getBookingDate())
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .durationMinutes(request.getDurationMinutes())
                .pricePerHour(resolveStartPrice(subField, request))
                .subFieldPrice(subFieldPrice)
                .bookingPrice(bookingPrice)
                .platformBookingFee(bookingPrice)
                .bookingType(BookingType.NORMAL)
                .paymentMethod(paymentMethod)
                .status(BookingStatus.PENDING)
                .paymentStatus(BookingPaymentStatus.UNPAID)
                .paymentExpiresAt(paymentExpiresAt)
                .note(request.getNote())
                .sourceRecurringBookingId(sourceRecurringBookingId)
                .build();

        Booking saved = transactionTemplate.execute(status -> savePendingBooking(userId, subField, booking, !recurringOccurrence));
        if (saved == null) {
            throw new IllegalStateException("Pending booking transaction did not return a booking");
        }
        BalanceDeductionResponse deduction = confirmWithAccountBalanceIfPossible(saved);
        if (recurringOccurrence) {
            notifyRecurringAutomaticPaymentResult(saved, deduction);
        }
        return bookingMapper.toResponse(saved, subField);
    }

    private BookingResponse createReservationWithExistingValidation(UUID ownerId, CreateBookingRequest request) {
        SubFieldResponse subField = subFieldProjectionService.getRequiredSubField(request.getSubFieldId());
        validateOwnerOwnsSubField(ownerId, subField);
        normalizeRequestDateTimes(request);
        validateBooking(subField, request, null);

        Booking booking = reservationBuilder(ownerId, request, subField).build();
        Booking saved = transactionTemplate.execute(status -> saveReservation(booking, subField, "created"));
        if (saved == null) {
            throw new IllegalStateException("Reservation transaction did not return a booking");
        }
        return bookingMapper.toResponse(saved, subField);
    }

    private BookingResponse updateReservationWithExistingValidation(UUID ownerId, UUID reservationId, CreateBookingRequest request) {
        Booking current = getOwnedReservation(ownerId, reservationId);
        SubFieldResponse subField = subFieldProjectionService.getRequiredSubField(request.getSubFieldId());
        validateOwnerOwnsSubField(ownerId, subField);
        normalizeRequestDateTimes(request);
        validateBookingForUpdate(subField, request, reservationId);

        current.setSubFieldId(subField.getId());
        current.setOwnerId(ownerId);
        current.setBookingDate(request.getBookingDate());
        current.setStartDateTime(request.getStartDateTime());
        current.setEndDateTime(request.getEndDateTime());
        current.setStartTime(request.getStartTime());
        current.setEndTime(request.getEndTime());
        current.setDurationMinutes(request.getDurationMinutes());
        current.setPricePerHour(BigDecimal.ZERO);
        current.setSubFieldPrice(BigDecimal.ZERO);
        current.setBookingPrice(0L);
        current.setPlatformBookingFee(0L);
        current.setPaymentStatus(BookingPaymentStatus.NOT_REQUIRED);
        current.setPaymentExpiresAt(null);
        current.setNote(request.getNote());

        Booking saved = transactionTemplate.execute(status -> saveReservation(current, subField, "updated"));
        if (saved == null) {
            throw new IllegalStateException("Reservation transaction did not return a booking");
        }
        return bookingMapper.toResponse(saved, subField);
    }

    private Booking.BookingBuilder reservationBuilder(UUID ownerId, CreateBookingRequest request, SubFieldResponse subField) {
        return Booking.builder()
                .bookingCode(BookingCodeGenerator.generate())
                .clientId(ownerId)
                .subFieldId(subField.getId())
                .ownerId(ownerId)
                .bookingDate(request.getBookingDate())
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .durationMinutes(request.getDurationMinutes())
                .pricePerHour(BigDecimal.ZERO)
                .subFieldPrice(BigDecimal.ZERO)
                .bookingPrice(0L)
                .platformBookingFee(0L)
                .bookingType(BookingType.RESERVATION)
                .paymentMethod(PaymentMethod.ACCOUNT_BALANCE)
                .status(BookingStatus.CONFIRMED)
                .paymentStatus(BookingPaymentStatus.NOT_REQUIRED)
                .paymentExpiresAt(null)
                .note(request.getNote());
    }

    private Booking saveReservation(Booking booking, SubFieldResponse subField, String action) {
        Booking saved;
        try {
            saved = bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            if (isBookingOverlapConstraintViolation(ex)) {
                throw new BookingConflictException(BOOKING_CONFLICT_MESSAGE);
            }
            throw ex;
        }
        availabilityCacheService.evict(saved.getSubFieldId(), saved.getBookingDate());
        bookingNotificationEventPublisher.publishReservationChanged(saved, subField, action.toUpperCase());
        log.info("Reservation {}: reservationId={}, ownerId={}, fieldId={}, subFieldId={}, bookingType={}",
                action, saved.getId(), saved.getOwnerId(), subField.getFieldId(), saved.getSubFieldId(), saved.getBookingType());
        return saved;
    }

    private Booking savePendingBooking(UUID userId, SubFieldResponse subField, Booking booking, boolean failOnReservationConflict) {
        Booking saved;
        try {
            saved = bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            if (isBookingOverlapConstraintViolation(ex)) {
                if (hasSameClientBooking(booking)) {
                    throw new BadRequestException("You have already booked this field successfully.", "BOOKING_ALREADY_EXISTS");
                }
                throw new BookingConflictException(BOOKING_CONFLICT_MESSAGE);
            }
            throw ex;
        }
        if (!pendingBookingReservationService.reserve(userId, saved.getId(), saved.getPaymentExpiresAt()) && failOnReservationConflict) {
            throw new BadRequestException("You already have a booking waiting for payment. Please complete or wait for it to expire before creating another booking.", "BOOKING_ALREADY_EXISTS");
        }
        log.info("Booking created: code={}, clientId={}, subFieldId={}", saved.getBookingCode(), userId, subField.getId());
        availabilityCacheService.evict(saved.getSubFieldId(), saved.getBookingDate());
        bookingNotificationEventPublisher.publishBookingCreated(saved, subField, null);
        return saved;
    }

    private boolean hasSameClientBooking(Booking booking) {
        return bookingRepository.existsByClientIdAndSubFieldIdAndStartDateTimeAndEndDateTimeAndStatusIn(
                booking.getClientId(),
                booking.getSubFieldId(),
                booking.getStartDateTime(),
                booking.getEndDateTime(),
                RESERVING_STATUSES);
    }

    @Override
    @Transactional
    public BookingResponse payPendingBooking(UUID userId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        if (!booking.getClientId().equals(userId)) {
            throw new UnauthorizedException("You are not authorised to pay for this booking");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Booking is not waiting for payment", "BOOKING_CANNOT_MODIFY");
        }
        if (booking.getPaymentExpiresAt() != null && !booking.getPaymentExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Booking payment window has expired", "BOOKING_EXPIRED");
        }
        bookingModerationService.ensureCanBook(userId, resolveFieldId(booking));
        BalanceDeductionResponse deduction = payPendingBookingFromWallet(booking);
        if (!deduction.deducted()) {
            throw new BadRequestException("Insufficient account balance", "INSUFFICIENT_BALANCE");
        }
        if (booking.getSourceRecurringBookingId() != null && deduction.balance() == 0L) {
            bookingNotificationEventPublisher.publishRecurringPaymentWalletEmpty(booking);
        }
        Booking updated = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        return bookingMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID userId, CancelBookingRequest request) {
        int updatedRows = bookingRepository.cancelClientBooking(
                request.getBookingId(), userId, CANCELLABLE_STATUSES, BookingStatus.CANCELLED,
                request.getReason(), LocalDateTime.now(), BookingCancelledBy.CLIENT,
                BookingPaymentStatus.PAID, BookingPaymentStatus.REFUNDED, BookingPaymentStatus.FAILED);

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
        pendingBookingReservationService.release(cancelled);
        availabilityCacheService.evict(cancelled.getSubFieldId(), cancelled.getBookingDate());
        bookingNotificationEventPublisher.publishBookingCancelled(cancelled, null);
        return bookingMapper.toResponse(cancelled);
    }

    @Override
    @Transactional
    public BookingResponse cancelBookingByManager(UUID managerId, String managerRole, CancelBookingRequest request) {
        Booking current = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(request.getBookingId()));
        validateManagerCanAccessField(managerId, managerRole, current);
        int updatedRows = bookingRepository.cancelManagerBooking(
                request.getBookingId(), CANCELLABLE_STATUSES, BookingStatus.CANCELLED,
                request.getReason(), LocalDateTime.now(), BookingCancelledBy.OWNER,
                BookingPaymentStatus.PAID, BookingPaymentStatus.REFUNDED, BookingPaymentStatus.FAILED);

        if (updatedRows == 0) {
            throw new BookingNotCancellableException(current.getId(), current.getStatus().name());
        }

        Booking cancelled = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(request.getBookingId()));
        log.info("Booking cancelled by field manager: id={}, managerId={}, role={}", cancelled.getId(), managerId, managerRole);
        publishRefundIfEligible(cancelled);
        pendingBookingReservationService.release(cancelled);
        availabilityCacheService.evict(cancelled.getSubFieldId(), cancelled.getBookingDate());
        bookingNotificationEventPublisher.publishBookingCancelled(cancelled, null);
        return bookingMapper.toResponse(cancelled);
    }

    @Override
    @Transactional
    public int expirePendingBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiringBookings = bookingRepository.findPendingBookingsExpiringAtOrBefore(BookingStatus.PENDING, now);
        int expiredCount = bookingRepository.expirePendingBookings(
                BookingStatus.PENDING, BookingStatus.EXPIRED, now,
                PAYMENT_TIMEOUT_REASON, now, BookingCancelledBy.SYSTEM,
                BookingPaymentStatus.PAID, BookingPaymentStatus.REFUNDED, BookingPaymentStatus.FAILED);
        if (expiredCount > 0) {
            pauseRecurringBookingsForExpiredPayments(expiringBookings);
            availabilityCacheService.evictAll();
            log.info("Expired {} pending bookings at or before {}", expiredCount, now);
        }
        return expiredCount;
    }

    private void pauseRecurringBookingsForExpiredPayments(List<Booking> expiringBookings) {
        expiringBookings.stream()
                .filter(booking -> booking.getSourceRecurringBookingId() != null)
                .forEach(booking -> bookingRepository.findById(booking.getId())
                        .filter(expired -> expired.getStatus() == BookingStatus.EXPIRED)
                        .ifPresent(this::pauseRecurringBookingForExpiredPayment));
    }

    private void pauseRecurringBookingForExpiredPayment(Booking booking) {
        int changed = recurringBookingRepository.updateStatus(
                booking.getSourceRecurringBookingId(),
                RecurringBookingStatus.ACTIVE,
                RecurringBookingStatus.PAUSED);
        pendingBookingReservationService.release(booking);
        if (changed == 1) {
            refreshHasRecurring(booking.getSubFieldId());
        }
        bookingNotificationEventPublisher.publishRecurringPausedPaymentTimeout(booking);
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
            finishedBookings.stream()
                    .filter(booking -> booking.getBookingType() == BookingType.NORMAL)
                    .forEach(bookingTrustEventPublisher::publishBookingCompleted);
            log.info("Completed {} confirmed bookings ending on or before {}", completedCount, now);
        }
        return completedCount;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getMyBookings(UUID userId, LocalDate bookingDate, BookingStatus status, Pageable pageable) {
        LocalDateTime bookingDateStart = bookingDate == null ? null : bookingDate.atStartOfDay();
        LocalDateTime bookingDateEnd = bookingDate == null ? null : bookingDate.plusDays(1).atStartOfDay();
        return PageResponse.from(bookingRepository.findClientBookings(
                userId,
                bookingDateStart,
                bookingDateEnd,
                status,
                pageable).map(bookingMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getManagerBookings(UUID managerId, String managerRole, LocalDate bookingDate, UUID fieldId, SportType fieldType, SubFieldType subFieldType, BookingStatus status, Pageable pageable) {
        LocalDateTime bookingDateStart = bookingDate == null ? LocalDateTime.of(1970,1,1,0,0) : bookingDate.atStartOfDay();
        LocalDateTime bookingDateEnd = bookingDate == null ? LocalDateTime.of(9999,12,31,0,0) : bookingDate.plusDays(1).atStartOfDay();
        Collection<SubFieldType> subFieldTypes = resolveSubFieldTypes(fieldType, subFieldType);
        boolean filterSubFieldTypes = fieldType != null || subFieldType != null;
        List<UUID> managedFieldIds = "EMPLOYEE".equals(managerRole) ? fieldManagementClient.assignedFieldIds(managerId) : List.of();
        if ("EMPLOYEE".equals(managerRole) && managedFieldIds.isEmpty()) {
            return PageResponse.from(Page.empty(pageable));
        }
        Page<Booking> page = "EMPLOYEE".equals(managerRole)
                ? bookingRepository.findEmployeeManagedBookings(
                        managedFieldIds, bookingDateStart, bookingDateEnd, fieldId, filterSubFieldTypes, subFieldTypes, status, pageable)
                : bookingRepository.findOwnerBookings(
                        managerId, bookingDateStart, bookingDateEnd, fieldId, filterSubFieldTypes, subFieldTypes, status, pageable);
        Page<BookingResponse> responses = page.map(bookingMapper::toResponse);
        enrichClientProfiles(responses.getContent());
        Map<UUID, MatchResultResponse> resultByBookingId = matchResultRepository.findByBookingIdIn(
                        responses.getContent().stream().map(BookingResponse::getId).toList())
                .stream()
                .collect(Collectors.toMap(MatchResult::getBookingId, bookingMapper::toMatchResultResponse));
        responses.getContent().forEach(response -> response.setMatchResult(resultByBookingId.get(response.getId())));
        return PageResponse.from(responses);
    }

    private Collection<SubFieldType> resolveSubFieldTypes(SportType fieldType, SubFieldType subFieldType) {
        if (subFieldType != null) {
            return List.of(subFieldType);
        }
        if (fieldType != null) {
            return SubFieldType.forFieldType(fieldType);
        }
        return Arrays.asList(SubFieldType.values());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getManagerReservations(UUID managerId, String managerRole, LocalDate bookingDate, UUID subFieldId, BookingStatus status, Pageable pageable) {
        LocalDateTime bookingDateStart = bookingDate == null ? null : bookingDate.atStartOfDay();
        LocalDateTime bookingDateEnd = bookingDate == null ? null : bookingDate.plusDays(1).atStartOfDay();
        List<UUID> managedFieldIds = "EMPLOYEE".equals(managerRole) ? fieldManagementClient.assignedFieldIds(managerId) : List.of();
        if ("EMPLOYEE".equals(managerRole) && managedFieldIds.isEmpty()) {
            return PageResponse.from(Page.empty(pageable));
        }
        Page<Booking> page = "EMPLOYEE".equals(managerRole)
                ? bookingRepository.findEmployeeManagedReservations(
                        managedFieldIds, bookingDateStart, bookingDateEnd, subFieldId, status, BookingType.RESERVATION, pageable)
                : bookingRepository.findOwnerReservations(
                        managerId, bookingDateStart, bookingDateEnd, subFieldId, status, BookingType.RESERVATION, pageable);
        return PageResponse.from(page.map(bookingMapper::toResponse));
    }

    private void enrichClientProfiles(List<BookingResponse> responses) {
        if (responses.isEmpty()) {
            return;
        }
        Set<UUID> userIds = responses.stream()
                .map(BookingResponse::getClientId)
                .collect(Collectors.toCollection(HashSet::new));
        responses.stream()
                .map(BookingResponse::getOpponentId)
                .filter(Objects::nonNull)
                .forEach(userIds::add);
        Map<UUID, UserProjection> usersById = userProjectionRepository.findAllById(
                        userIds)
                .stream()
                .collect(Collectors.toMap(UserProjection::getUserId, projection -> projection));
        responses.forEach(response -> {
            UserProjection client = usersById.get(response.getClientId());
            if (client != null) {
                response.setClientName(client.getFullName());
                response.setClientPhoneNumber(client.getPhoneNumber());
                response.setClientAvatarUrl(client.getAvatarUrl());
            }
            UserProjection opponent = usersById.get(response.getOpponentId());
            if (opponent != null) {
                response.setOpponentName(opponent.getFullName());
                response.setOpponentPhoneNumber(opponent.getPhoneNumber());
            }
        });
    }

    private void validateManagerCanAccessField(UUID managerId, String managerRole, Booking booking) {
        if ("OWNER".equals(managerRole) && booking.getOwnerId().equals(managerId)) {
            return;
        }
        UUID fieldId = booking.getSubField() != null ? booking.getSubField().getFieldId() : null;
        if ("EMPLOYEE".equals(managerRole) && fieldId != null && fieldManagementClient.canManageField(managerId, managerRole, fieldId)) {
            return;
        }
        throw new UnauthorizedException("You are not authorised to manage this booking");
    }

    private UUID resolveFieldId(Booking booking) {
        return booking.getSubField() != null ? booking.getSubField().getFieldId() : null;
    }

    private Booking getOwnedReservation(UUID ownerId, UUID reservationId) {
        Booking booking = bookingRepository.findById(reservationId)
                .orElseThrow(() -> new BookingNotFoundException(reservationId));
        if (!ownerId.equals(booking.getOwnerId()) || booking.getBookingType() != BookingType.RESERVATION) {
            log.warn("Permission denied for reservation: userId={}, ownerId={}, fieldId={}, subFieldId={}, reservationId={}, bookingType={}",
                    ownerId, booking.getOwnerId(), resolveFieldId(booking), booking.getSubFieldId(), reservationId, booking.getBookingType());
            throw new UnauthorizedException("You are not authorised to manage this reservation");
        }
        return booking;
    }

    private void validateOwnerOwnsSubField(UUID ownerId, SubFieldResponse subField) {
        if (!ownerId.equals(subField.getOwnerId())) {
            log.warn("Permission denied for reservation: userId={}, ownerId={}, fieldId={}, subFieldId={}, bookingType={}",
                    ownerId, subField.getOwnerId(), subField.getFieldId(), subField.getId(), BookingType.RESERVATION);
            throw new UnauthorizedException("You are not authorised to reserve this field");
        }
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
        ResolvedOperatingHours hours = subFieldProjectionService.resolveOperatingHours(
                subFieldId, subField.getFieldId(), date.getDayOfWeek());

        LocalDate scheduleStartDate = date.minusDays(1);
        LocalDate scheduleEndDate = date.plusDays(7);
        LocalDateTime dayStart = scheduleStartDate.atStartOfDay();
        LocalDateTime dayEnd = scheduleEndDate.plusDays(1).atStartOfDay();
        Set<LocalDate> closedDates = findClosedDates(subFieldId, scheduleStartDate, scheduleEndDate);
        boolean selectedDateClosed = closedDates.contains(date);
        List<Booking> existingBookings = bookingRepository
                .findOverlappingBookings(subFieldId, dayStart, dayEnd, RESERVING_STATUSES);
        List<UnavailableSlotResponse> unavailableSlots = existingBookings.stream()
                .map(booking -> UnavailableSlotResponse.builder()
                        .startTime(booking.getStartTime())
                        .endTime(booking.getEndTime())
                        .startDateTime(booking.getStartDateTime())
                        .endDateTime(booking.getEndDateTime())
                        .build())
                .collect(Collectors.toList());

        if (Boolean.TRUE.equals(subField.getHasRecurring())) {
            scheduleStartDate.datesUntil(scheduleEndDate.plusDays(1))
                    .forEach(occurrenceDate -> addRecurringUnavailableSlots(unavailableSlots, subFieldId, occurrenceDate));
        }

        return AvailabilityResponse.builder()
                .openTime(hours.closed() || selectedDateClosed ? null : hours.openTime())
                .closeTime(hours.closed() || selectedDateClosed ? null : hours.closeTime())
                .open24Hours(!selectedDateClosed && hours.open24Hours())
                .operatingHours(buildAvailabilityOperatingHours(subField, scheduleStartDate, scheduleEndDate, closedDates))
                .unavailableSlots(unavailableSlots)
                .build();
    }

    private void addRecurringUnavailableSlots(
            List<UnavailableSlotResponse> unavailableSlots,
            UUID subFieldId,
            LocalDate occurrenceDate) {
        recurringBookingRepository.findActiveReservationsForDate(
                subFieldId, occurrenceDate, RecurringBookingStatus.ACTIVE)
                .stream()
                .map(recurring -> UnavailableSlotResponse.builder()
                        .startTime(recurring.getStartTime())
                        .endTime(recurring.getEndTime())
                        .startDateTime(LocalDateTime.of(occurrenceDate, recurring.getStartTime()))
                        .endDateTime(LocalDateTime.of(
                                recurring.getEndTime().isAfter(recurring.getStartTime())
                                        ? occurrenceDate
                                        : occurrenceDate.plusDays(1),
                                recurring.getEndTime()))
                        .build())
                .forEach(unavailableSlots::add);
    }

    private List<AvailabilityOperatingHoursResponse> buildAvailabilityOperatingHours(
            SubFieldResponse subField,
            LocalDate startDate,
            LocalDate endDate,
            Set<LocalDate> closedDates) {
        return startDate.datesUntil(endDate.plusDays(1))
                .map(day -> {
                    ResolvedOperatingHours resolved = subFieldProjectionService.resolveOperatingHours(
                            subField.getId(), subField.getFieldId(), day.getDayOfWeek());
                    boolean closed = resolved.closed() || closedDates.contains(day);
                    return AvailabilityOperatingHoursResponse.builder()
                            .date(day)
                            .openTime(closed ? null : resolved.openTime())
                            .closeTime(closed ? null : resolved.closeTime())
                            .closed(closed)
                            .open24Hours(!closed && resolved.open24Hours())
                            .build();
                })
                .toList();
    }

    private Set<LocalDate> findClosedDates(UUID subFieldId, LocalDate startDate, LocalDate endDate) {
        List<SubFieldClosureProjection> closures = fieldClosureProjectionRepository.findOverlappingDateRange(
                subFieldId, startDate, endDate);
        if (closures == null || closures.isEmpty()) {
            return Set.of();
        }
        Set<LocalDate> closedDates = new HashSet<>();
        for (SubFieldClosureProjection closure : closures) {
            LocalDate effectiveStart = closure.getStartDate().isBefore(startDate) ? startDate : closure.getStartDate();
            LocalDate effectiveEnd = closure.getEndDate().isAfter(endDate) ? endDate : closure.getEndDate();
            effectiveStart.datesUntil(effectiveEnd.plusDays(1)).forEach(closedDates::add);
        }
        return closedDates;
    }

    private void publishRefundIfEligible(Booking booking) {
        BookingConfig config = bookingConfigService.getConfig();
        long refundAmount = booking.getBookingPrice() == null || booking.getBookingPrice() == 0L
                ? (booking.getPlatformBookingFee() == null ? 0L : booking.getPlatformBookingFee())
                : booking.getBookingPrice();
        if (!Boolean.TRUE.equals(config.getRefundEnabled()) || refundAmount <= 0) {
            return;
        }
        if (booking.getPaymentStatus() != BookingPaymentStatus.PAID) {
            log.info("Skipping booking fee refund because booking payment is not paid: bookingId={}, paymentStatus={}",
                    booking.getId(), booking.getPaymentStatus());
            return;
        }
        LocalDateTime refundDeadline = bookingStartDateTime(booking).minusHours(config.getRefundBeforeHours());
        if (LocalDateTime.now().isBefore(refundDeadline)) {
            bookingBalanceEventPublisher.publishRefundRequested(booking, refundAmount, BOOKING_PAYMENT_REFUND_REASON);
            bookingRepository.markPaymentRefunded(booking.getId(), BookingPaymentStatus.REFUNDED);
        }
    }

    private long resolveBookingPrice(UUID userId) {
        int completedBookingCount = userProjectionRepository.findById(userId)
                .map(projection -> projection.getCompletedBookingCount() == null ? 0 : projection.getCompletedBookingCount())
                .orElse(0);

        BookingConfig config = bookingConfigService.getConfig();
        return completedBookingCount == 0 ? config.getFirstBookingFee() : config.getNotFirstBookingFee();
    }

    private void ensureNoPendingPayment(UUID userId) {
        if (pendingBookingReservationService.find(userId).isPresent()
                || bookingRepository.existsByClientIdAndStatus(userId, BookingStatus.PENDING)) {
            throw new BadRequestException("You already have a booking waiting for payment. Please complete or wait for it to expire before creating another booking.", "BOOKING_ALREADY_EXISTS");
        }
    }

    private BalanceDeductionResponse confirmWithAccountBalanceIfPossible(Booking booking) {
        long payableAmount = payableBookingAmount(booking);
        if (payableAmount <= 0) {
            confirmPendingBookingImmediately(booking, null);
            return new BalanceDeductionResponse(true, 0L, "No payment required");
        }
        BalanceDeductionResponse deduction = deductBalanceWithRetry(booking, balanceDeductionRequest(booking, payableAmount));
        if (!deduction.deducted()) {
            if (booking.getSourceRecurringBookingId() == null) {
                expireNormalBookingAfterInsufficientBalance(booking);
                log.info("Normal booking rejected due to insufficient balance: bookingId={}, balance={}",
                        booking.getId(), deduction.balance());
                throw new BadRequestException(INSUFFICIENT_BALANCE_REASON, "INSUFFICIENT_BALANCE");
            }
            log.info("Booking remains pending due to insufficient balance: bookingId={}, balance={}", booking.getId(), deduction.balance());
            return deduction;
        }
        try {
            confirmPendingBookingImmediately(booking, null);
            log.info("Confirmed booking from synchronous wallet deduction: bookingId={}", booking.getId());
        } catch (RuntimeException ex) {
            log.error("Balance was deducted but immediate booking confirmation failed; payment success inbox will retry: bookingId={}",
                    booking.getId(), ex);
        }
        return deduction;
    }

    private void expireNormalBookingAfterInsufficientBalance(Booking booking) {
        transactionTemplate.execute(status -> {
            LocalDateTime now = LocalDateTime.now();
            int changed = bookingRepository.cancelClientBooking(
                    booking.getId(), booking.getClientId(), List.of(BookingStatus.PENDING), BookingStatus.EXPIRED,
                    INSUFFICIENT_BALANCE_REASON, now, BookingCancelledBy.SYSTEM,
                    BookingPaymentStatus.PAID, BookingPaymentStatus.REFUNDED, BookingPaymentStatus.FAILED);
            if (changed != 1) {
                return null;
            }
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setCancellationReason(INSUFFICIENT_BALANCE_REASON);
            booking.setCancelledAt(now);
            booking.setCancelledBy(BookingCancelledBy.SYSTEM);
            booking.setPaymentStatus(BookingPaymentStatus.FAILED);
            pendingBookingReservationService.release(booking);
            availabilityCacheService.evict(booking.getSubFieldId(), booking.getBookingDate());
            return null;
        });
    }

    private BalanceDeductionResponse payPendingBookingFromWallet(Booking booking) {
        long payableAmount = payableBookingAmount(booking);
        if (payableAmount <= 0) {
            confirmPendingBookingImmediately(booking, null);
            return new BalanceDeductionResponse(true, 0L, "No payment required");
        }
        BalanceDeductionResponse deduction = deductBalanceWithRetry(booking, balanceDeductionRequest(booking, payableAmount));
        if (deduction.deducted()) {
            confirmPendingBookingImmediately(booking, null);
        }
        return deduction;
    }

    private BalanceDeductionRequest balanceDeductionRequest(Booking booking, long payableAmount) {
        return new BalanceDeductionRequest(
                booking.getClientId(),
                payableAmount,
                booking.getId(),
                booking.getBookingCode(),
                BOOKING_PAYMENT_REASON);
    }

    private void notifyRecurringAutomaticPaymentResult(Booking booking, BalanceDeductionResponse deduction) {
        if (deduction.deducted()) {
            if (deduction.balance() == 0L) {
                bookingNotificationEventPublisher.publishRecurringPaymentWalletEmpty(booking);
            }
            return;
        }
        bookingNotificationEventPublisher.publishRecurringPaymentFailed(booking);
    }

    private BalanceDeductionResponse deductBalanceWithRetry(Booking booking, BalanceDeductionRequest request) {
        BalanceDeductionResponse deduction = null;
        int maxAttempts = Math.max(1, balanceDeductionAttempts);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            deduction = userBalanceClient.deduct(request);
            if (deduction.deducted() || attempt == maxAttempts) {
                return deduction;
            }
            log.info("Retrying wallet deduction after insufficient balance: bookingId={}, attempt={}, balance={}",
                    booking.getId(), attempt, deduction.balance());
            sleepBeforeBalanceRetry(booking);
        }
        return deduction;
    }

    private void sleepBeforeBalanceRetry(Booking booking) {
        try {
            Thread.sleep(Math.max(0L, balanceDeductionRetryDelayMs));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Wallet deduction retry interrupted: bookingId={}", booking.getId());
        }
    }

    private void confirmPendingBookingImmediately(Booking booking, String userEmail) {
        Boolean confirmed = transactionTemplate.execute(status -> {
            int changed = bookingRepository.confirmPendingBookingFromPayment(
                    booking.getId(), BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingPaymentStatus.PAID);
            if (changed != 1) {
                return false;
            }
            Booking confirmedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
            pendingBookingReservationService.release(confirmedBooking);
            availabilityCacheService.evict(confirmedBooking.getSubFieldId(), confirmedBooking.getBookingDate());
            bookingNotificationEventPublisher.publishBookingConfirmed(confirmedBooking, userEmail);
            return true;
        });
        if (Boolean.TRUE.equals(confirmed)) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaymentStatus(BookingPaymentStatus.PAID);
        }
    }

    private long payableBookingAmount(Booking booking) {
        return booking.getBookingPrice() == null || booking.getBookingPrice() == 0L
                ? (booking.getPlatformBookingFee() == null ? 0L : booking.getPlatformBookingFee())
                : booking.getBookingPrice();
    }

    private LocalDateTime bookingStartDateTime(Booking booking) {
        return booking.getStartDateTime() != null
                ? booking.getStartDateTime()
                : LocalDateTime.of(booking.getBookingDate(), booking.getStartTime());
    }

    private void validateBooking(SubFieldResponse subField, CreateBookingRequest request, UUID sourceRecurringBookingId) {
        validateBooking(subField, request, sourceRecurringBookingId, true);
    }

    private void validateBooking(
            SubFieldResponse subField,
            CreateBookingRequest request,
            UUID sourceRecurringBookingId,
            boolean enforceFutureDateLimit) {
        validateSubFieldActive(subField);
        if (enforceFutureDateLimit) {
            validateBookingDateWindow(request.getBookingDate());
        } else {
            validateBookingDateNotPast(request.getBookingDate());
        }
        validateBookingStartNotPast(request.getStartDateTime());
        validateDuration(request, subField);
        validateStartTimeAlignment(request.getStartTime());
        validateClosureDate(subField.getId(), request.getStartDateTime().toLocalDate(), request.getEndDateTime().toLocalDate());
        ResolvedOperatingHours hours = subFieldProjectionService.resolveOperatingHours(
                subField.getId(), subField.getFieldId(), request.getBookingDate().getDayOfWeek());
        validateWithinOperatingHours(
                subField.getId(), subField.getFieldId(), request.getStartDateTime(), request.getEndDateTime(), hours);
        validateNoConflict(request.getSubFieldId(), request.getStartDateTime(), request.getEndDateTime(),
                Boolean.TRUE.equals(subField.getHasRecurring()), sourceRecurringBookingId);
    }

    private void validateBookingForUpdate(SubFieldResponse subField, CreateBookingRequest request, UUID bookingId) {
        validateSubFieldActive(subField);
        validateBookingDateWindow(request.getBookingDate());
        validateBookingStartNotPast(request.getStartDateTime());
        validateDuration(request, subField);
        validateStartTimeAlignment(request.getStartTime());
        validateClosureDate(subField.getId(), request.getStartDateTime().toLocalDate(), request.getEndDateTime().toLocalDate());
        ResolvedOperatingHours hours = subFieldProjectionService.resolveOperatingHours(
                subField.getId(), subField.getFieldId(), request.getBookingDate().getDayOfWeek());
        validateWithinOperatingHours(
                subField.getId(), subField.getFieldId(), request.getStartDateTime(), request.getEndDateTime(), hours);
        validateNoConflictExcludingBooking(request.getSubFieldId(), request.getStartDateTime(), request.getEndDateTime(),
                Boolean.TRUE.equals(subField.getHasRecurring()), bookingId);
    }

    private void normalizeRequestDateTimes(CreateBookingRequest request) {
        if (request.getStartDateTime() == null) {
            if (request.getBookingDate() == null || request.getStartTime() == null) {
                throw new BadRequestException("Start date/time is required");
            }
            request.setStartDateTime(LocalDateTime.of(request.getBookingDate(), request.getStartTime()));
        }

        if (request.getEndDateTime() == null) {
            request.setEndDateTime(request.getStartDateTime().plusMinutes(request.getDurationMinutes()));
        }

        request.setBookingDate(request.getStartDateTime().toLocalDate());
        request.setStartTime(request.getStartDateTime().toLocalTime());
        request.setEndTime(LocalTime.MIDNIGHT.equals(request.getEndDateTime().toLocalTime())
                ? LocalTime.of(23, 59)
                : request.getEndDateTime().toLocalTime());
    }

    private void validateSubFieldActive(SubFieldResponse subField) {
        if (Boolean.FALSE.equals(subField.getActive()) || !"ACTIVE".equalsIgnoreCase(subField.getStatus())) {
            throw new BadRequestException("SubField '" + subField.getName() + "' is not currently available for booking", "FIELD_NOT_AVAILABLE");
        }
    }

    private void validateClosureDate(UUID subFieldId, LocalDate startDate, LocalDate endDate) {
        boolean closed = fieldClosureProjectionRepository.existsOverlappingDateRange(subFieldId, startDate, endDate);
        if (closed) {
            throw new BadRequestException(SUBFIELD_CLOSED_MESSAGE, SUBFIELD_CLOSED_CODE);
        }
    }

    private void validateBookingDateNotPast(LocalDate bookingDate) {
        if (bookingDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Booking date cannot be in the past");
        }
    }

    private void validateBookingDateWindow(LocalDate bookingDate) {
        validateBookingDateNotPast(bookingDate);
        LocalDate latestBookingDate = LocalDate.now().plusDays(maxBookingDaysInFuture);
        if (bookingDate.isAfter(latestBookingDate)) {
            throw new BadRequestException(
                    "Booking date cannot be more than " + maxBookingDaysInFuture + " days in the future",
                    "BOOKING_DATE_OUT_OF_RANGE");
        }
    }

    private void validateBookingStartNotPast(LocalDateTime startDateTime) {
        if (!startDateTime.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Booking start time must be in the future");
        }
    }

    private void validateWithinOperatingHours(UUID subFieldId, UUID fieldId, LocalDateTime startDateTime, LocalDateTime endDateTime,
                                              ResolvedOperatingHours startDateHours) {
        LocalDateTime cursor = startDateTime;
        while (cursor.isBefore(endDateTime)) {
            ResolvedOperatingHours hours = cursor.toLocalDate().equals(startDateTime.toLocalDate())
                    ? startDateHours
                    : subFieldProjectionService.resolveOperatingHours(subFieldId, fieldId, cursor.getDayOfWeek());
            LocalDateTime windowEnd = operatingWindowEnd(cursor, hours);
            if (!cursor.isBefore(windowEnd)) {
                throwOutsideOperatingHours(hours);
            }
            cursor = endDateTime.isBefore(windowEnd) ? endDateTime : windowEnd;
        }
    }

    private LocalDateTime operatingWindowEnd(LocalDateTime cursor, ResolvedOperatingHours hours) {
        if (hours.closed()) {
            throw new BadRequestException(SUBFIELD_CLOSED_MESSAGE, SUBFIELD_CLOSED_CODE);
        }
        if (hours.open24Hours()) {
            return cursor.toLocalDate().plusDays(1).atStartOfDay();
        }
        LocalTime openTime = hours.openTime();
        LocalTime closeTime = hours.closeTime();
        if (openTime == null || closeTime == null) {
            throwOutsideOperatingHours(hours);
        }
        LocalDateTime windowStart = LocalDateTime.of(cursor.toLocalDate(), openTime);
        LocalDateTime windowEnd = LocalDateTime.of(cursor.toLocalDate(), closeTime);
        if (LocalTime.of(23, 59).equals(closeTime)) {
            windowEnd = windowEnd.plusMinutes(1);
        }
        if (!closeTime.isAfter(openTime)) {
            windowEnd = windowEnd.plusDays(1);
            if (cursor.toLocalTime().isBefore(closeTime)) {
                windowStart = windowStart.minusDays(1);
                windowEnd = windowEnd.minusDays(1);
            }
        }
        if (cursor.isBefore(windowStart) || !cursor.isBefore(windowEnd)) {
            throwOutsideOperatingHours(hours);
        }
        return windowEnd;
    }

    private void throwOutsideOperatingHours(ResolvedOperatingHours hours) {
        throw new BadRequestException("Booking time must be within operating hours: "
                + hours.openTime() + " - " + hours.closeTime());
    }

    private BigDecimal resolveStartPrice(SubFieldResponse subField, CreateBookingRequest request) {
        if (subField.getTimePriceRules() == null || subField.getTimePriceRules().isEmpty()) {
            throw new BadRequestException("Time price rules are not configured for this sub-field");
        }
        LocalTime startTime = request.getStartDateTime() != null
                ? request.getStartDateTime().toLocalTime()
                : request.getStartTime();
        return subField.getTimePriceRules().stream()
                .filter(rule -> isWithinRule(startTime, rule))
                .findFirst()
                .map(TimePriceRuleDto::getHourlyPrice)
                .orElseThrow(() -> new BadRequestException(
                        "Time price rules do not cover requested booking start " + request.getStartDateTime()
                                + ". Configured rules: " + describeTimePriceRules(subField.getTimePriceRules())));
    }

    private boolean isWithinRule(LocalTime time, TimePriceRuleDto rule) {
        LocalTime start = rule.getStartTime();
        LocalTime end = END_OF_DAY_TIME.equals(rule.getEndTime()) ? LocalTime.MIDNIGHT : rule.getEndTime();
        if (LocalTime.MIDNIGHT.equals(end) && !LocalTime.MIDNIGHT.equals(start)) {
            return !time.isBefore(start);
        }
        if (end.isAfter(start)) {
            return !time.isBefore(start) && time.isBefore(end);
        }
        return !time.isBefore(start) || time.isBefore(end);
    }

    private String describeTimePriceRules(List<TimePriceRuleDto> rules) {
        return rules.stream()
                .map(rule -> rule.getStartTime() + "-" + rule.getEndTime())
                .toList()
                .toString();
    }

    private void validateDuration(CreateBookingRequest request, SubFieldResponse subField) {
        if (!request.getEndDateTime().isAfter(request.getStartDateTime())) {
            throw new BadRequestException("End time must be after start time");
        }
        long actualMinutes = java.time.Duration.between(request.getStartDateTime(), request.getEndDateTime()).toMinutes();
        if (actualMinutes != request.getDurationMinutes()) {
            throw new BadRequestException("Duration must match start and end timestamps");
        }
        int minMinutes = resolveMinimumBookingMinutes(subField);
        int maxMinutes = resolveMaximumBookingMinutes(subField);
        if (actualMinutes < minMinutes) {
            throw new BadRequestException("Minimum booking duration for this sub-field is " + minMinutes + " minutes");
        }
        if (actualMinutes > maxMinutes) {
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

    private void validateNoConflict(
            UUID subFieldId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            boolean hasActiveRecurring,
            UUID sourceRecurringBookingId) {
        LocalTime legacyEndTime = LocalTime.MIDNIGHT.equals(endDateTime.toLocalTime())
                ? LocalTime.of(23, 59)
                : endDateTime.toLocalTime();
        boolean isConflict = sourceRecurringBookingId == null
                ? bookingRepository.existsConflictingBookings(
                        subFieldId,
                        startDateTime.toLocalDate(),
                        startDateTime.toLocalTime(),
                        legacyEndTime,
                        RESERVING_STATUSES)
                : bookingRepository.existsConflictingBookings(
                        subFieldId,
                        startDateTime.toLocalDate(),
                        startDateTime.toLocalTime(),
                        legacyEndTime,
                        RESERVING_STATUSES,
                        sourceRecurringBookingId);
        boolean recurringConflict = hasActiveRecurring
                && !recurringBookingRepository.findActiveConflictsForDate(
                        subFieldId,
                        startDateTime.toLocalDate(),
                        startDateTime.toLocalTime(),
                        endDateTime.toLocalTime(),
                        RecurringBookingStatus.ACTIVE,
                        sourceRecurringBookingId).isEmpty();
        if (isConflict || recurringConflict) {
            throw new BookingConflictException(BOOKING_CONFLICT_MESSAGE);
        }
    }

    private void validateNoConflictExcludingBooking(
            UUID subFieldId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            boolean hasActiveRecurring,
            UUID bookingId) {
        boolean isConflict = bookingRepository.existsConflictingBookingsExcludingBooking(
                subFieldId,
                startDateTime,
                endDateTime,
                RESERVING_STATUSES,
                bookingId);
        boolean recurringConflict = hasActiveRecurring
                && !recurringBookingRepository.findActiveConflictsForDate(
                        subFieldId,
                        startDateTime.toLocalDate(),
                        startDateTime.toLocalTime(),
                        endDateTime.toLocalTime(),
                        RecurringBookingStatus.ACTIVE,
                        null).isEmpty();
        if (isConflict || recurringConflict) {
            throw new BookingConflictException(BOOKING_CONFLICT_MESSAGE);
        }
    }

    private void refreshHasRecurring(UUID subFieldId) {
        boolean hasRecurring = recurringBookingRepository.existsBySubFieldIdAndStatus(subFieldId, RecurringBookingStatus.ACTIVE);
        bookingSubFieldProjectionRepository.updateHasRecurring(subFieldId, hasRecurring);
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
