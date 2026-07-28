package com.project.booking.service.impl;

import com.project.booking.cache.AvailabilityCacheService;
import com.project.booking.client.FieldManagementClient;
import com.project.booking.client.UserBalanceClient;
import com.project.booking.community.service.CommunityPostMaintenanceService;
import com.project.booking.config.BookingDatabaseConstraints;
import com.project.booking.dto.request.CancelBookingRequest;
import com.project.booking.dto.request.CreateBookingRequest;
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
import com.project.common.enums.PaymentMethod;
import com.project.common.enums.RecurringBookingStatus;
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
import java.util.List;
import java.util.Map;
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
    private static final String BOOKING_PAYMENT_REFUND_REASON = "BOOKING_PAYMENT_REFUND";
    private static final String BOOKING_PAYMENT_REASON = "BOOKING_ACCOUNT_BALANCE_PAYMENT";
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
    private final CommunityPostMaintenanceService communityPostMaintenanceService;
    private final UserProjectionRepository userProjectionRepository;
    private final BookingTrustEventPublisher bookingTrustEventPublisher;
    private final BookingModerationService bookingModerationService;
    private final MatchResultRepository matchResultRepository;
    private final PendingBookingReservationService pendingBookingReservationService;
    private final TransactionTemplate transactionTemplate;
    private final BookingLockManager bookingLockManager;
    private final FieldManagementClient fieldManagementClient;

    @Value("${booking.payment-timeout-minutes:35}")
    private int paymentTimeoutMinutes = 35;

    @Override
    public BookingResponse createBooking(UUID userId, CreateBookingRequest request) {
        return createBooking(userId, request, null);
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
        bookingModerationService.ensureCanBook(userId, subField.getFieldId());
        normalizeRequestDateTimes(request);
        validateBooking(subField, request, recurringBookingId);
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
        bookingModerationService.ensureCanBook(userId, subField.getFieldId());
        normalizeRequestDateTimes(request);

        validateBooking(subField, request, sourceRecurringBookingId);

        long bookingPrice = resolveBookingPrice(userId);
        PaymentMethod paymentMethod = PaymentMethod.ACCOUNT_BALANCE;
        if (pendingBookingReservationService.find(userId).isPresent()
                || bookingRepository.findFirstByClientIdAndStatusOrderByCreatedAtAsc(userId, BookingStatus.PENDING).isPresent()) {
            throw new BadRequestException("You already have a booking waiting for payment. Please complete or wait for it to expire before creating another booking.");
        }
        BigDecimal subFieldPrice = pricingStrategy.calculate(subField, request);
        LocalDateTime paymentExpiresAt = LocalDateTime.now().plusMinutes(paymentTimeoutMinutes);
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
                .totalAmount(subFieldPrice)
                .subFieldPrice(subFieldPrice)
                .bookingPrice(bookingPrice)
                .platformBookingFee(bookingPrice)
                .paymentMethod(paymentMethod)
                .status(BookingStatus.PENDING)
                .paymentStatus(BookingPaymentStatus.UNPAID)
                .paymentExpiresAt(paymentExpiresAt)
                .note(request.getNote())
                .sourceRecurringBookingId(sourceRecurringBookingId)
                .build();

        Booking saved = transactionTemplate.execute(status -> savePendingBooking(userId, subField, booking));
        if (saved == null) {
            throw new IllegalStateException("Pending booking transaction did not return a booking");
        }
        confirmWithAccountBalanceIfPossible(saved);
        return bookingMapper.toResponse(saved, subField);
    }

    private Booking savePendingBooking(UUID userId, SubFieldResponse subField, Booking booking) {
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
        if (!pendingBookingReservationService.reserve(userId, saved.getId(), saved.getPaymentExpiresAt())) {
            throw new BadRequestException("You already have a booking waiting for payment. Please complete or wait for it to expire before creating another booking.");
        }
        log.info("Booking created: code={}, clientId={}, subFieldId={}", saved.getBookingCode(), userId, subField.getId());
        availabilityCacheService.evict(saved.getSubFieldId(), saved.getBookingDate());
        bookingNotificationEventPublisher.publishBookingCreated(saved, subField, null);
        return saved;
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
        communityPostMaintenanceService.cancelOpenPostForBooking(cancelled.getId());
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
        communityPostMaintenanceService.cancelOpenPostForBooking(cancelled.getId());
        bookingNotificationEventPublisher.publishBookingCancelled(cancelled, null);
        return bookingMapper.toResponse(cancelled);
    }

    @Override
    @Transactional
    public int expirePendingBookings() {
        LocalDateTime now = LocalDateTime.now();
        int expiredCount = bookingRepository.expirePendingBookings(
                BookingStatus.PENDING, BookingStatus.EXPIRED, now,
                PAYMENT_TIMEOUT_REASON, now, BookingCancelledBy.SYSTEM,
                BookingPaymentStatus.PAID, BookingPaymentStatus.REFUNDED, BookingPaymentStatus.FAILED);
        if (expiredCount > 0) {
            availabilityCacheService.evictAll();
            log.info("Expired {} pending bookings at or before {}", expiredCount, now);
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
    public PageResponse<BookingResponse> getManagerBookings(UUID managerId, String managerRole, LocalDate bookingDate, UUID subFieldId, BookingStatus status, Pageable pageable) {
        LocalDateTime bookingDateStart = bookingDate == null ? null : bookingDate.atStartOfDay();
        LocalDateTime bookingDateEnd = bookingDate == null ? null : bookingDate.plusDays(1).atStartOfDay();
        List<UUID> managedFieldIds = "EMPLOYEE".equals(managerRole) ? fieldManagementClient.assignedFieldIds(managerId) : List.of();
        if ("EMPLOYEE".equals(managerRole) && managedFieldIds.isEmpty()) {
            return PageResponse.from(Page.empty(pageable));
        }
        Page<Booking> page = "EMPLOYEE".equals(managerRole)
                ? bookingRepository.findEmployeeManagedBookings(
                        managedFieldIds, bookingDateStart, bookingDateEnd, subFieldId, status, pageable)
                : bookingRepository.findOwnerBookings(
                        managerId, bookingDateStart, bookingDateEnd, subFieldId, status, pageable);
        Page<BookingResponse> responses = page.map(bookingMapper::toResponse);
        enrichClientProfiles(responses.getContent());
        Map<UUID, MatchResultResponse> resultByBookingId = matchResultRepository.findByBookingIdIn(
                        responses.getContent().stream().map(BookingResponse::getId).toList())
                .stream()
                .collect(Collectors.toMap(MatchResult::getBookingId, bookingMapper::toMatchResultResponse));
        responses.getContent().forEach(response -> response.setMatchResult(resultByBookingId.get(response.getId())));
        return PageResponse.from(responses);
    }

    private void enrichClientProfiles(List<BookingResponse> responses) {
        if (responses.isEmpty()) {
            return;
        }
        Map<UUID, UserProjection> usersById = userProjectionRepository.findAllById(
                        responses.stream()
                                .map(BookingResponse::getClientId)
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(UserProjection::getUserId, projection -> projection));
        responses.forEach(response -> {
            UserProjection client = usersById.get(response.getClientId());
            if (client != null) {
                response.setClientName(client.getFullName());
                response.setClientPhoneNumber(client.getPhoneNumber());
                response.setClientAvatarUrl(client.getAvatarUrl());
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

        LocalDate scheduleStartDate = date.minusDays(1);
        LocalDate scheduleEndDate = date.plusDays(7);
        LocalDateTime dayStart = scheduleStartDate.atStartOfDay();
        LocalDateTime dayEnd = scheduleEndDate.plusDays(1).atStartOfDay();
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
                .openTime(hours.closed() ? null : hours.openTime())
                .closeTime(hours.closed() ? null : hours.closeTime())
                .open24Hours(hours.open24Hours())
                .operatingHours(buildAvailabilityOperatingHours(subFieldId, scheduleStartDate, scheduleEndDate))
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
            UUID subFieldId,
            LocalDate startDate,
            LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1))
                .map(day -> {
                    ResolvedOperatingHours resolved = subFieldProjectionService.resolveOperatingHours(
                            subFieldId, day.getDayOfWeek());
                    return AvailabilityOperatingHoursResponse.builder()
                            .date(day)
                            .openTime(resolved.closed() ? null : resolved.openTime())
                            .closeTime(resolved.closed() ? null : resolved.closeTime())
                            .closed(resolved.closed())
                            .open24Hours(resolved.open24Hours())
                            .build();
                })
                .toList();
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

    private void confirmWithAccountBalanceIfPossible(Booking booking) {
        long payableAmount = payableBookingAmount(booking);
        if (payableAmount <= 0) {
            confirmPendingBookingImmediately(booking, null);
            return;
        }
        BalanceDeductionResponse deduction = userBalanceClient.deduct(new BalanceDeductionRequest(
                booking.getClientId(),
                payableAmount,
                booking.getId(),
                booking.getBookingCode(),
                BOOKING_PAYMENT_REASON));
        if (!deduction.deducted()) {
            log.info("Booking remains pending due to insufficient balance: bookingId={}, balance={}", booking.getId(), deduction.balance());
            return;
        }
        try {
            confirmPendingBookingImmediately(booking, null);
            log.info("Confirmed booking from synchronous wallet deduction: bookingId={}", booking.getId());
        } catch (RuntimeException ex) {
            log.error("Balance was deducted but immediate booking confirmation failed; payment success inbox will retry: bookingId={}",
                    booking.getId(), ex);
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
        validateSubFieldActive(subField);
        validateBookingStartNotPast(request.getStartDateTime());
        validateClosureDate(subField.getId(), request.getStartDateTime().toLocalDate(), request.getEndDateTime().toLocalDate());
        ResolvedOperatingHours hours = subFieldProjectionService.resolveOperatingHours(
                subField.getId(), request.getBookingDate().getDayOfWeek());
        validateWithinOperatingHours(subField.getId(), request.getStartDateTime(), request.getEndDateTime(), hours);
        validateDuration(request, subField);
        validateStartTimeAlignment(request.getStartTime());
        validateNoConflict(request.getSubFieldId(), request.getStartDateTime(), request.getEndDateTime(), sourceRecurringBookingId);
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
            throw new BadRequestException("SubField '" + subField.getName() + "' is not currently available for booking");
        }
    }

    private void validateClosureDate(UUID subFieldId, LocalDate startDate, LocalDate endDate) {
        boolean closed = fieldClosureProjectionRepository.existsOverlappingDateRange(subFieldId, startDate, endDate);
        if (closed) {
            throw new BadRequestException("SubField is closed on the selected booking date");
        }
    }

    private void validateBookingDateNotPast(LocalDate bookingDate) {
        if (bookingDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Booking date cannot be in the past");
        }
    }

    private void validateBookingStartNotPast(LocalDateTime startDateTime) {
        if (!startDateTime.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Booking start time must be in the future");
        }
    }

    private void validateWithinOperatingHours(UUID subFieldId, LocalDateTime startDateTime, LocalDateTime endDateTime,
                                              ResolvedOperatingHours startDateHours) {
        LocalDateTime cursor = startDateTime;
        while (cursor.isBefore(endDateTime)) {
            ResolvedOperatingHours hours = cursor.toLocalDate().equals(startDateTime.toLocalDate())
                    ? startDateHours
                    : subFieldProjectionService.resolveOperatingHours(subFieldId, cursor.getDayOfWeek());
            LocalDateTime windowEnd = operatingWindowEnd(cursor, hours);
            if (!cursor.isBefore(windowEnd)) {
                throwOutsideOperatingHours(hours);
            }
            cursor = endDateTime.isBefore(windowEnd) ? endDateTime : windowEnd;
        }
    }

    private LocalDateTime operatingWindowEnd(LocalDateTime cursor, ResolvedOperatingHours hours) {
        if (hours.closed()) {
            throw new BadRequestException("SubField is closed on the selected booking date");
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

    private void validateNoConflict(UUID subFieldId, LocalDateTime startDateTime, LocalDateTime endDateTime, UUID sourceRecurringBookingId) {
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
        boolean recurringConflict = !recurringBookingRepository.findActiveConflictsForDate(
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
