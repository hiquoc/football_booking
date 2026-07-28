package com.project.booking.service.impl;

import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.request.CancelBookingRequest;
import com.project.booking.dto.response.AvailabilityResponse;
import com.project.booking.dto.response.BookingResponse;
import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.dto.response.TimePriceRuleDto;
import com.project.booking.cache.AvailabilityCacheService;
import com.project.booking.client.UserBalanceClient;
import com.project.booking.community.service.CommunityPostMaintenanceService;
import com.project.booking.entity.Booking;
import com.project.booking.exception.BookingConflictException;
import com.project.booking.kafka.BookingNotificationEventPublisher;
import com.project.booking.kafka.BookingTrustEventPublisher;
import com.project.booking.lock.BookingLockManager;
import com.project.booking.mapper.BookingMapper;
import com.project.booking.moderation.service.BookingModerationService;
import com.project.booking.entity.BookingConfig;
import com.project.booking.kafka.BookingBalanceEventPublisher;
import com.project.booking.pricing.PricingStrategy;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.FieldClosureProjectionRepository;
import com.project.booking.repository.MatchResultRepository;
import com.project.booking.repository.RecurringBookingRepository;
import com.project.booking.repository.UserProjectionRepository;
import com.project.booking.entity.UserProjection;
import com.project.booking.service.ResolvedOperatingHours;
import com.project.booking.service.BookingConfigService;
import com.project.booking.service.PendingBookingReservationService;
import com.project.booking.service.SubFieldProjectionService;
import com.project.common.enums.BookingCancelledBy;
import com.project.common.enums.BookingPaymentStatus;
import com.project.common.enums.BookingStatus;
import com.project.common.enums.PaymentMethod;
import com.project.common.dto.balance.BalanceDeductionResponse;
import com.project.common.exception.BadRequestException;
import com.project.booking.exception.BookingInProgressException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SubFieldProjectionService subFieldProjectionService;

    @Mock
    private FieldClosureProjectionRepository fieldClosureProjectionRepository;

    @Mock
    private PricingStrategy pricingStrategy;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private BookingNotificationEventPublisher bookingNotificationEventPublisher;

    @Mock
    private BookingConfigService bookingConfigService;

    @Mock
    private BookingBalanceEventPublisher bookingBalanceEventPublisher;

    @Mock
    private UserBalanceClient userBalanceClient;

    @Mock
    private AvailabilityCacheService availabilityCacheService;

    @Mock
    private RecurringBookingRepository recurringBookingRepository;

    @Mock
    private CommunityPostMaintenanceService communityPostMaintenanceService;

    @Mock
    private UserProjectionRepository userProjectionRepository;

    @Mock
    private BookingTrustEventPublisher bookingTrustEventPublisher;

    @Mock
    private BookingModerationService bookingModerationService;

    @Mock
    private MatchResultRepository matchResultRepository;

    @Mock
    private PendingBookingReservationService pendingBookingReservationService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private BookingLockManager bookingLockManager;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(transactionTemplate.execute(org.mockito.ArgumentMatchers.any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(null);
                });
        org.mockito.Mockito.lenient().when(bookingConfigService.getConfig()).thenReturn(BookingConfig.builder()
                .firstBookingFee(5000L)
                .notFirstBookingFee(1000L)
                .refundBeforeHours(24)
                .refundEnabled(true)
                .build());
        org.mockito.Mockito.lenient().when(userBalanceClient.deduct(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new BalanceDeductionResponse(false, 0L, "Insufficient account balance"));
        org.mockito.Mockito.lenient().when(pendingBookingReservationService.find(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());
        org.mockito.Mockito.lenient().when(pendingBookingReservationService.reserve(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(true);
        org.mockito.Mockito.lenient().when(recurringBookingRepository.findActiveConflictsForDate(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(userProjectionRepository.findById(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());
        org.mockito.Mockito.lenient().when(bookingLockManager.executeWithLock(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    java.util.function.Supplier<?> supplier = invocation.getArgument(3);
                    return supplier.get();
                });
    }

    @Test
    void createBookingDerivesEndTimeFromRequestedDuration() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        SubFieldResponse subField = activeSubField(subFieldId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(90)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), eq(request.getBookingDate().getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(request.getBookingDate()),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(10, 0)), anyCollection())).thenReturn(false);
        when(pricingStrategy.calculate(eq(subField), eq(request))).thenReturn(new BigDecimal("150000"));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingMapper.toResponse(any(Booking.class), eq(subField))).thenReturn(BookingResponse.builder().build());

        bookingService.createBooking(userId, request);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveAndFlush(bookingCaptor.capture());
        Booking savedBooking = bookingCaptor.getValue();
        assertEquals(LocalTime.of(8, 30), savedBooking.getStartTime());
        assertEquals(LocalTime.of(10, 0), savedBooking.getEndTime());
        assertEquals(90, savedBooking.getDurationMinutes());
    }

    @Test
    void createBookingStoresConfiguredFeeSeparatelyAndRequestsWalletDeduction() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        SubFieldResponse subField = activeSubField(subFieldId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(60)
                .paymentMethod(PaymentMethod.ACCOUNT_BALANCE)
                .build();

        when(bookingConfigService.getConfig()).thenReturn(BookingConfig.builder()
                .firstBookingFee(6000L)
                .notFirstBookingFee(2000L)
                .refundBeforeHours(24)
                .refundEnabled(true)
                .build());
        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), eq(request.getBookingDate().getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(request.getBookingDate()),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(9, 30)), anyCollection())).thenReturn(false);
        when(pricingStrategy.calculate(eq(subField), eq(request))).thenReturn(new BigDecimal("100000"));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingMapper.toResponse(any(Booking.class), eq(subField))).thenReturn(BookingResponse.builder().build());

        bookingService.createBooking(userId, request);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveAndFlush(bookingCaptor.capture());
        Booking savedBooking = bookingCaptor.getValue();
        assertEquals(new BigDecimal("100000"), savedBooking.getTotalAmount());
        assertEquals(6000L, savedBooking.getPlatformBookingFee());
        assertEquals(PaymentMethod.ACCOUNT_BALANCE, savedBooking.getPaymentMethod());
        assertEquals(BookingStatus.PENDING, savedBooking.getStatus());
        verify(userBalanceClient).deduct(any());
    }

    @Test
    void createBookingReturnsPendingWhenBalanceDeductedButImmediateConfirmationFails() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        SubFieldResponse subField = activeSubField(subFieldId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(60)
                .build();
        BookingResponse pendingResponse = BookingResponse.builder()
                .status(BookingStatus.PENDING)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), eq(request.getBookingDate().getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(request.getBookingDate()),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(9, 30)), anyCollection())).thenReturn(false);
        when(pricingStrategy.calculate(eq(subField), eq(request))).thenReturn(new BigDecimal("100000"));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userBalanceClient.deduct(any())).thenReturn(new BalanceDeductionResponse(true, 10000L, "Balance deducted"));
        when(bookingRepository.confirmPendingBookingFromPayment(any(), eq(BookingStatus.PENDING), eq(BookingStatus.CONFIRMED), eq(BookingPaymentStatus.PAID)))
                .thenThrow(new DataIntegrityViolationException("temporary confirmation failure"));
        when(bookingMapper.toResponse(any(Booking.class), eq(subField))).thenReturn(pendingResponse);

        BookingResponse response = bookingService.createBooking(userId, request);

        assertEquals(BookingStatus.PENDING, response.getStatus());
        verify(userBalanceClient).deduct(any());
        verify(bookingRepository).confirmPendingBookingFromPayment(any(), eq(BookingStatus.PENDING), eq(BookingStatus.CONFIRMED), eq(BookingPaymentStatus.PAID));
        verify(bookingNotificationEventPublisher, never()).publishBookingConfirmed(any(), any());
    }

    @Test
    void createBookingUsesNotFirstBookingFeeForReturningClients() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        SubFieldResponse subField = activeSubField(subFieldId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(60)
                .build();

        when(userProjectionRepository.findById(userId)).thenReturn(Optional.of(UserProjection.builder()
                .userId(userId)
                .completedBookingCount(3)
                .build()));
        when(bookingConfigService.getConfig()).thenReturn(BookingConfig.builder()
                .firstBookingFee(6000L)
                .notFirstBookingFee(2000L)
                .refundBeforeHours(24)
                .refundEnabled(true)
                .build());
        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), eq(request.getBookingDate().getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(request.getBookingDate()),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(9, 30)), anyCollection())).thenReturn(false);
        when(pricingStrategy.calculate(eq(subField), eq(request))).thenReturn(new BigDecimal("100000"));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingMapper.toResponse(any(Booking.class), eq(subField))).thenReturn(BookingResponse.builder().build());

        bookingService.createBooking(userId, request);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveAndFlush(bookingCaptor.capture());
        assertEquals(2000L, bookingCaptor.getValue().getPlatformBookingFee());
    }

    @Test
    void createBookingTreatsMidnightEndTimeAsEndOfDay() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        SubFieldResponse subField = activeSubFieldUntilEndOfDay(subFieldId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(23, 0))
                .durationMinutes(60)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), eq(request.getBookingDate().getDayOfWeek())))
                .thenReturn(new ResolvedOperatingHours(LocalTime.of(6, 0), LocalTime.of(23, 59), false));
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(request.getBookingDate()),
                eq(LocalTime.of(23, 0)), eq(LocalTime.of(23, 59)), anyCollection())).thenReturn(false);
        when(pricingStrategy.calculate(eq(subField), eq(request))).thenReturn(new BigDecimal("100000"));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingMapper.toResponse(any(Booking.class), eq(subField))).thenReturn(BookingResponse.builder().build());

        bookingService.createBooking(userId, request);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveAndFlush(bookingCaptor.capture());
        Booking savedBooking = bookingCaptor.getValue();
        assertEquals(LocalTime.of(23, 0), savedBooking.getStartTime());
        assertEquals(LocalTime.of(23, 59), savedBooking.getEndTime());
        assertEquals(60, savedBooking.getDurationMinutes());
    }

    @Test
    void resolveStartPriceTreats2359RuleEndAsMidnightBoundary() {
        UUID subFieldId = UUID.randomUUID();
        SubFieldResponse subField = activeSubFieldUntilEndOfDay(subFieldId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .startDateTime(LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(23, 59)))
                .startTime(LocalTime.of(23, 59))
                .build();

        BigDecimal price = ReflectionTestUtils.invokeMethod(bookingService, "resolveStartPrice", subField, request);

        assertEquals(new BigDecimal("100000"), price);
    }

    @Test
    void createBookingAcceptsMidnightStartTime() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate bookingDate = LocalDate.now().plusDays(1);
        SubFieldResponse subField = activeSubFieldAllDay(subFieldId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(bookingDate)
                .startTime(LocalTime.MIDNIGHT)
                .durationMinutes(60)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), eq(bookingDate.getDayOfWeek())))
                .thenReturn(new ResolvedOperatingHours(LocalTime.MIDNIGHT, LocalTime.of(23, 59), false, true));
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(bookingDate),
                eq(LocalTime.MIDNIGHT), eq(LocalTime.of(1, 0)), anyCollection())).thenReturn(false);
        when(pricingStrategy.calculate(eq(subField), eq(request))).thenReturn(new BigDecimal("100000"));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingMapper.toResponse(any(Booking.class), eq(subField))).thenReturn(BookingResponse.builder().build());

        bookingService.createBooking(userId, request);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveAndFlush(bookingCaptor.capture());
        assertEquals(LocalTime.MIDNIGHT, bookingCaptor.getValue().getStartTime());
        assertEquals(LocalTime.of(1, 0), bookingCaptor.getValue().getEndTime());
    }

    @Test
    void createBookingAcceptsCrossMidnightOperatingHours() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate bookingDate = LocalDate.now().plusDays(1);
        SubFieldResponse subField = activeSubFieldOvernight(subFieldId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .startDateTime(LocalDateTime.of(bookingDate, LocalTime.of(18, 0)))
                .endDateTime(LocalDateTime.of(bookingDate.plusDays(1), LocalTime.of(2, 0)))
                .durationMinutes(480)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), eq(bookingDate.getDayOfWeek())))
                .thenReturn(new ResolvedOperatingHours(LocalTime.of(18, 0), LocalTime.of(2, 0), false, false));
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(bookingDate),
                eq(LocalTime.of(18, 0)), eq(LocalTime.of(2, 0)), anyCollection())).thenReturn(false);
        when(pricingStrategy.calculate(eq(subField), eq(request))).thenReturn(new BigDecimal("1440000"));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingMapper.toResponse(any(Booking.class), eq(subField))).thenReturn(BookingResponse.builder().build());

        bookingService.createBooking(userId, request);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveAndFlush(bookingCaptor.capture());
        Booking savedBooking = bookingCaptor.getValue();
        assertEquals(bookingDate, savedBooking.getBookingDate());
        assertEquals(LocalTime.of(18, 0), savedBooking.getStartTime());
        assertEquals(LocalTime.of(2, 0), savedBooking.getEndTime());
        assertEquals(480, savedBooking.getDurationMinutes());
    }

    @Test
    void createBookingAcceptsBookingAcrossAdjacentDailyOperatingWindows() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate sunday = LocalDate.now();
        while (sunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
            sunday = sunday.plusDays(1);
        }
        SubFieldResponse subField = activeSubFieldUntilEndOfDay(subFieldId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .startDateTime(LocalDateTime.of(sunday, LocalTime.of(23, 0)))
                .endDateTime(LocalDateTime.of(sunday.plusDays(1), LocalTime.of(1, 30)))
                .durationMinutes(150)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), eq(DayOfWeek.SUNDAY)))
                .thenReturn(new ResolvedOperatingHours(LocalTime.of(6, 0), LocalTime.of(23, 59), false));
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), eq(DayOfWeek.MONDAY)))
                .thenReturn(new ResolvedOperatingHours(null, null, false, true));
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(sunday),
                eq(LocalTime.of(23, 0)), eq(LocalTime.of(1, 30)), anyCollection())).thenReturn(false);
        when(pricingStrategy.calculate(eq(subField), eq(request))).thenReturn(new BigDecimal("250000"));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingMapper.toResponse(any(Booking.class), eq(subField))).thenReturn(BookingResponse.builder().build());

        bookingService.createBooking(userId, request);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveAndFlush(bookingCaptor.capture());
        Booking savedBooking = bookingCaptor.getValue();
        assertEquals(sunday, savedBooking.getBookingDate());
        assertEquals(LocalTime.of(23, 0), savedBooking.getStartTime());
        assertEquals(LocalTime.of(1, 30), savedBooking.getEndTime());
        assertEquals(150, savedBooking.getDurationMinutes());
    }

    @Test
    void createBookingRejectsStartTimeOutsideThirtyMinuteBoundary() {
        UUID subFieldId = UUID.randomUUID();
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 15))
                .durationMinutes(60)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(activeSubField(subFieldId));
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), eq(request.getBookingDate().getDayOfWeek())))
                .thenReturn(openHours());

        assertThrows(BadRequestException.class, () -> bookingService.createBooking(UUID.randomUUID(), request));
        verify(bookingRepository, never()).existsConflictingBookings(any(), any(), any(), any(), anyCollection());
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void createBookingRejectsConflictingRange() {
        UUID subFieldId = UUID.randomUUID();
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(60)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(activeSubField(subFieldId));
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), eq(request.getBookingDate().getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(request.getBookingDate()),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(9, 30)), anyCollection())).thenReturn(true);

        assertThrows(BookingConflictException.class, () -> bookingService.createBooking(UUID.randomUUID(), request));
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void createBookingMapsDatabaseOverlapConstraintViolationToBookingConflict() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        SubFieldResponse subField = activeSubField(subFieldId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(90)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), eq(request.getBookingDate().getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(request.getBookingDate()),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(10, 0)), anyCollection())).thenReturn(false);
        when(pricingStrategy.calculate(eq(subField), eq(request))).thenReturn(new BigDecimal("150000"));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenThrow(new DataIntegrityViolationException(
                "violates exclusion constraint \"bookings_no_overlapping_active_bookings\""));

        BookingConflictException exception = assertThrows(BookingConflictException.class,
                () -> bookingService.createBooking(userId, request));

        assertEquals("The selected time slot is no longer available.", exception.getMessage());
    }

    @Test
    void createBookingUsesSubFieldDateLockBeforeExistingValidation() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate bookingDate = LocalDate.now().plusDays(1);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(bookingDate)
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(60)
                .build();

        org.mockito.Mockito.doThrow(new BookingInProgressException()).when(bookingLockManager).executeWithLock(
                eq(subFieldId),
                eq(LocalDateTime.of(bookingDate, LocalTime.of(8, 30))),
                eq(LocalDateTime.of(bookingDate, LocalTime.of(9, 30))),
                any());

        assertThrows(BookingInProgressException.class, () -> bookingService.createBooking(userId, request));
        verify(subFieldProjectionService, never()).getRequiredSubField(any());
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void getAvailabilityReturnsOperatingHoursAndBookedRanges() {
        UUID subFieldId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);
        SubFieldResponse subField = activeSubField(subFieldId);
        Booking firstBooking = Booking.builder()
                .subFieldId(subFieldId)
                .bookingDate(date)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .status(BookingStatus.CONFIRMED)
                .build();
        Booking secondBooking = Booking.builder()
                .subFieldId(subFieldId)
                .bookingDate(date)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .status(BookingStatus.PENDING)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any()))
                .thenReturn(openHours());
        when(bookingRepository.findOverlappingBookings(
                eq(subFieldId), any(LocalDateTime.class), any(LocalDateTime.class), anyCollection()))
                .thenReturn(List.of(firstBooking, secondBooking));

        AvailabilityResponse response = bookingService.getAvailability(subFieldId, date);

        assertEquals(LocalTime.of(6, 0), response.getOpenTime());
        assertEquals(LocalTime.of(23, 0), response.getCloseTime());
        assertEquals(2, response.getUnavailableSlots().size());
        assertEquals(LocalTime.of(9, 0), response.getUnavailableSlots().get(0).getStartTime());
        assertEquals(LocalTime.of(10, 30), response.getUnavailableSlots().get(0).getEndTime());
        assertEquals(LocalTime.of(14, 0), response.getUnavailableSlots().get(1).getStartTime());
        assertEquals(LocalTime.of(16, 0), response.getUnavailableSlots().get(1).getEndTime());
        assertEquals(9, response.getOperatingHours().size());
    }

    @Test
    void getAvailabilityReturnsOpenAllDayFlag() {
        UUID subFieldId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);
        SubFieldResponse subField = activeSubFieldAllDay(subFieldId);

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any()))
                .thenReturn(new ResolvedOperatingHours(null, null, false, true));
        when(bookingRepository.findOverlappingBookings(
                eq(subFieldId), any(LocalDateTime.class), any(LocalDateTime.class), anyCollection()))
                .thenReturn(List.of());

        AvailabilityResponse response = bookingService.getAvailability(subFieldId, date);

        assertEquals(true, response.getOpen24Hours());
        assertEquals(null, response.getOpenTime());
        assertEquals(null, response.getCloseTime());
    }

    @Test
    void expirePendingBookingsOnlyExpiresPendingBookingsOlderThanTimeout() {
        ReflectionTestUtils.setField(bookingService, "paymentTimeoutMinutes", 20);
        ArgumentCaptor<LocalDateTime> expiresBeforeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> cancelledAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        when(bookingRepository.expirePendingBookings(
                eq(BookingStatus.PENDING),
                eq(BookingStatus.EXPIRED),
                expiresBeforeCaptor.capture(),
                eq("Payment timeout"),
                cancelledAtCaptor.capture(),
                eq(BookingCancelledBy.SYSTEM),
                eq(BookingPaymentStatus.PAID),
                eq(BookingPaymentStatus.REFUNDED),
                eq(BookingPaymentStatus.FAILED))).thenReturn(3);

        int expiredCount = bookingService.expirePendingBookings();

        assertEquals(3, expiredCount);
        assertEquals(cancelledAtCaptor.getValue(), expiresBeforeCaptor.getValue());
    }

    @Test
    void completeFinishedBookingsOnlyTransitionsConfirmedBookings() {
        List<Booking> finishedBookings = List.of(
                Booking.builder().id(UUID.randomUUID()).status(BookingStatus.CONFIRMED).build(),
                Booking.builder().id(UUID.randomUUID()).status(BookingStatus.CONFIRMED).build(),
                Booking.builder().id(UUID.randomUUID()).status(BookingStatus.CONFIRMED).build(),
                Booking.builder().id(UUID.randomUUID()).status(BookingStatus.CONFIRMED).build());
        when(bookingRepository.findFinishedConfirmedBookings(
                eq(BookingStatus.CONFIRMED),
                any(LocalDate.class),
                any(LocalTime.class))).thenReturn(finishedBookings);

        int completedCount = bookingService.completeFinishedBookings();

        assertEquals(4, completedCount);
        finishedBookings.forEach(booking -> assertEquals(BookingStatus.COMPLETED, booking.getStatus()));
        verify(bookingRepository).saveAll(finishedBookings);
        finishedBookings.forEach(booking -> verify(bookingTrustEventPublisher).publishBookingCompleted(booking));
    }

    @Test
    void getManagerBookingsIncludesClientProfileFromReplica() {
        UUID ownerId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .clientId(clientId)
                .ownerId(ownerId)
                .subFieldId(UUID.randomUUID())
                .build();
        BookingResponse mapped = BookingResponse.builder()
                .id(bookingId)
                .clientId(clientId)
                .build();

        when(bookingRepository.findOwnerBookings(eq(ownerId), eq(null), eq(null), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(booking), PageRequest.of(0, 10), 1));
        when(bookingMapper.toResponse(booking)).thenReturn(mapped);
        when(userProjectionRepository.findAllById(any())).thenReturn(List.of(UserProjection.builder()
                .userId(clientId)
                .fullName("Nguyen Van A")
                .phoneNumber("0862470050")
                .avatarUrl("https://example.com/avatar.png")
                .build()));
        when(matchResultRepository.findByBookingIdIn(List.of(bookingId))).thenReturn(List.of());

        var response = bookingService.getManagerBookings(ownerId, "OWNER", null, null, null, PageRequest.of(0, 10));

        assertEquals("Nguyen Van A", response.getContent().getFirst().getClientName());
        assertEquals("0862470050", response.getContent().getFirst().getClientPhoneNumber());
        assertEquals("https://example.com/avatar.png", response.getContent().getFirst().getClientAvatarUrl());
    }

    @Test
    void cancelBookingPublishesBookingFeeRefundWhenEligible() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        CancelBookingRequest request = CancelBookingRequest.builder()
                .bookingId(bookingId)
                .reason("Change of plans")
                .build();
        Booking cancelled = Booking.builder()
                .id(bookingId)
                .clientId(userId)
                .bookingCode("BK-1")
                .bookingDate(LocalDate.now().plusDays(3))
                .startTime(LocalTime.of(8, 0))
                .platformBookingFee(2000L)
                .status(BookingStatus.CANCELLED)
                .paymentStatus(BookingPaymentStatus.PAID)
                .build();

        when(bookingConfigService.getConfig()).thenReturn(BookingConfig.builder()
                .firstBookingFee(5000L)
                .notFirstBookingFee(1000L)
                .refundBeforeHours(24)
                .refundEnabled(true)
                .build());
        when(bookingRepository.cancelClientBooking(
                eq(bookingId), eq(userId), anyCollection(), eq(BookingStatus.CANCELLED),
                eq("Change of plans"), any(LocalDateTime.class), eq(BookingCancelledBy.CLIENT),
                eq(BookingPaymentStatus.PAID), eq(BookingPaymentStatus.REFUNDED), eq(BookingPaymentStatus.FAILED))).thenReturn(1);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(cancelled));
        when(bookingMapper.toResponse(cancelled)).thenReturn(BookingResponse.builder().build());

        bookingService.cancelBooking(userId, request);

        verify(bookingBalanceEventPublisher).publishRefundRequested(cancelled, 2000L, "BOOKING_PAYMENT_REFUND");
        verify(bookingRepository).markPaymentRefunded(bookingId, BookingPaymentStatus.REFUNDED);
    }

    @Test
    void cancelBookingDoesNotRefundAfterConfiguredWindow() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        CancelBookingRequest request = CancelBookingRequest.builder()
                .bookingId(bookingId)
                .reason("Late cancellation")
                .build();
        Booking cancelled = Booking.builder()
                .id(bookingId)
                .clientId(userId)
                .bookingCode("BK-2")
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 0))
                .status(BookingStatus.CANCELLED)
                .paymentStatus(BookingPaymentStatus.PAID)
                .build();

        when(bookingConfigService.getConfig()).thenReturn(BookingConfig.builder()
                .firstBookingFee(5000L)
                .notFirstBookingFee(1000L)
                .refundBeforeHours(48)
                .refundEnabled(true)
                .build());
        when(bookingRepository.cancelClientBooking(
                eq(bookingId), eq(userId), anyCollection(), eq(BookingStatus.CANCELLED),
                eq("Late cancellation"), any(LocalDateTime.class), eq(BookingCancelledBy.CLIENT),
                eq(BookingPaymentStatus.PAID), eq(BookingPaymentStatus.REFUNDED), eq(BookingPaymentStatus.FAILED))).thenReturn(1);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(cancelled));
        when(bookingMapper.toResponse(cancelled)).thenReturn(BookingResponse.builder().build());

        bookingService.cancelBooking(userId, request);

        verify(bookingBalanceEventPublisher, never()).publishRefundRequested(any(), anyLong(), any());
    }

    private SubFieldResponse activeSubField(UUID subFieldId) {
        return SubFieldResponse.builder()
                .id(subFieldId)
                .ownerId(UUID.randomUUID())
                .name("Field A")
                .fieldName("Main Field")
                .status("ACTIVE")
                .active(true)
                .openTime(LocalTime.of(6, 0))
                .closeTime(LocalTime.of(23, 0))
                .minimumBookingDurationMinutes(30)
                .maximumBookingDurationMinutes(240)
                .timePriceRules(List.of(TimePriceRuleDto.builder()
                        .startTime(LocalTime.of(6, 0))
                        .endTime(LocalTime.of(23, 0))
                        .hourlyPrice(new BigDecimal("100000"))
                        .build()))
                .build();
    }

    private SubFieldResponse activeSubFieldUntilEndOfDay(UUID subFieldId) {
        return SubFieldResponse.builder()
                .id(subFieldId)
                .ownerId(UUID.randomUUID())
                .name("Field A")
                .fieldName("Main Field")
                .status("ACTIVE")
                .active(true)
                .openTime(LocalTime.of(6, 0))
                .closeTime(LocalTime.of(23, 59))
                .minimumBookingDurationMinutes(30)
                .maximumBookingDurationMinutes(240)
                .timePriceRules(List.of(TimePriceRuleDto.builder()
                        .startTime(LocalTime.of(6, 0))
                        .endTime(LocalTime.of(23, 59))
                        .hourlyPrice(new BigDecimal("100000"))
                        .build()))
                .build();
    }

    private SubFieldResponse activeSubFieldAllDay(UUID subFieldId) {
        return SubFieldResponse.builder()
                .id(subFieldId)
                .ownerId(UUID.randomUUID())
                .name("Field A")
                .fieldName("Main Field")
                .status("ACTIVE")
                .active(true)
                .minimumBookingDurationMinutes(30)
                .maximumBookingDurationMinutes(600)
                .timePriceRules(List.of(TimePriceRuleDto.builder()
                        .startTime(LocalTime.MIDNIGHT)
                        .endTime(LocalTime.of(23, 59))
                        .hourlyPrice(new BigDecimal("100000"))
                        .build()))
                .build();
    }

    private SubFieldResponse activeSubFieldOvernight(UUID subFieldId) {
        return SubFieldResponse.builder()
                .id(subFieldId)
                .ownerId(UUID.randomUUID())
                .name("Field A")
                .fieldName("Main Field")
                .status("ACTIVE")
                .active(true)
                .minimumBookingDurationMinutes(30)
                .maximumBookingDurationMinutes(600)
                .timePriceRules(List.of(TimePriceRuleDto.builder()
                        .startTime(LocalTime.of(18, 0))
                        .endTime(LocalTime.of(2, 0))
                        .hourlyPrice(new BigDecimal("180000"))
                        .build()))
                .build();
    }

    private ResolvedOperatingHours openHours() {
        return new ResolvedOperatingHours(LocalTime.of(6, 0), LocalTime.of(23, 0), false);
    }
}
