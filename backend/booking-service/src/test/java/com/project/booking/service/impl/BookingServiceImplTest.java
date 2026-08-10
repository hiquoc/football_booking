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
import com.project.booking.entity.SubFieldClosureProjection;
import com.project.booking.kafka.BookingBalanceEventPublisher;
import com.project.booking.pricing.PricingStrategy;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.BookingSubFieldProjectionRepository;
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
import com.project.common.enums.BookingType;
import com.project.common.enums.RecurringBookingStatus;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSubFieldProjectionRepository bookingSubFieldProjectionRepository;

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
        ReflectionTestUtils.setField(bookingService, "balanceDeductionAttempts", 1);
        ReflectionTestUtils.setField(bookingService, "balanceDeductionRetryDelayMs", 0L);
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
                .thenReturn(new BalanceDeductionResponse(true, 10000L, "Balance deducted"));
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
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(request.getBookingDate().getDayOfWeek())))
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
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(request.getBookingDate().getDayOfWeek())))
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
        assertEquals(new BigDecimal("100000"), savedBooking.getSubFieldPrice());
        assertEquals(6000L, savedBooking.getPlatformBookingFee());
        assertEquals(PaymentMethod.ACCOUNT_BALANCE, savedBooking.getPaymentMethod());
        assertEquals(BookingStatus.PENDING, savedBooking.getStatus());
        verify(userBalanceClient).deduct(any());
    }

    @Test
    void createRecurringOccurrenceKeepsPendingForThirtyMinutesWhenWalletIsInsufficient() {
        ReflectionTestUtils.setField(bookingService, "recurringPaymentTimeoutMinutes", 30);
        UUID userId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        SubFieldResponse subField = activeSubField(subFieldId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(60)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(request.getBookingDate().getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(request.getBookingDate()),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(9, 30)), anyCollection(), eq(recurringId))).thenReturn(false);
        when(pricingStrategy.calculate(eq(subField), eq(request))).thenReturn(new BigDecimal("100000"));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingMapper.toResponse(any(Booking.class), eq(subField))).thenReturn(BookingResponse.builder().status(BookingStatus.PENDING).build());
        when(userBalanceClient.deduct(any())).thenReturn(new BalanceDeductionResponse(false, 0L, "Insufficient account balance"));

        LocalDateTime before = LocalDateTime.now();
        BookingResponse response = bookingService.createRecurringOccurrence(userId, recurringId, request);
        LocalDateTime after = LocalDateTime.now();

        assertEquals(BookingStatus.PENDING, response.getStatus());
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveAndFlush(bookingCaptor.capture());
        Booking savedBooking = bookingCaptor.getValue();
        assertEquals(recurringId, savedBooking.getSourceRecurringBookingId());
        org.junit.jupiter.api.Assertions.assertFalse(savedBooking.getPaymentExpiresAt().isBefore(before.plusMinutes(30)));
        org.junit.jupiter.api.Assertions.assertFalse(savedBooking.getPaymentExpiresAt().isAfter(after.plusMinutes(30)));
        verify(bookingNotificationEventPublisher).publishRecurringPaymentFailed(savedBooking);
    }

    @Test
    void createBookingRejectsAndExpiresNormalBookingWhenWalletIsInsufficient() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        SubFieldResponse subField = activeSubField(subFieldId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(60)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(request.getBookingDate().getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(request.getBookingDate()),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(9, 30)), anyCollection())).thenReturn(false);
        when(pricingStrategy.calculate(eq(subField), eq(request))).thenReturn(new BigDecimal("100000"));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(UUID.randomUUID());
            return booking;
        });
        when(userBalanceClient.deduct(any())).thenReturn(new BalanceDeductionResponse(false, 0L, "Insufficient account balance"));
        when(bookingRepository.cancelClientBooking(
                any(), eq(userId), eq(List.of(BookingStatus.PENDING)), eq(BookingStatus.EXPIRED),
                eq("Insufficient account balance"), any(LocalDateTime.class), eq(BookingCancelledBy.SYSTEM),
                eq(BookingPaymentStatus.PAID), eq(BookingPaymentStatus.REFUNDED), eq(BookingPaymentStatus.FAILED)))
                .thenReturn(1);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> bookingService.createBooking(userId, request));

        assertEquals("INSUFFICIENT_BALANCE", exception.getCode());
        verify(bookingRepository).cancelClientBooking(
                any(), eq(userId), eq(List.of(BookingStatus.PENDING)), eq(BookingStatus.EXPIRED),
                eq("Insufficient account balance"), any(LocalDateTime.class), eq(BookingCancelledBy.SYSTEM),
                eq(BookingPaymentStatus.PAID), eq(BookingPaymentStatus.REFUNDED), eq(BookingPaymentStatus.FAILED));
        verify(pendingBookingReservationService).release(any(Booking.class));
        verify(availabilityCacheService, times(2)).evict(eq(subFieldId), eq(request.getBookingDate()));
        verify(bookingNotificationEventPublisher, never()).publishRecurringPaymentFailed(any());
    }

    @Test
    void payPendingBookingConfirmsRecurringBookingAndNotifiesWhenWalletBecomesEmpty() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .bookingCode("BK-1")
                .clientId(userId)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .status(BookingStatus.PENDING)
                .paymentStatus(BookingPaymentStatus.UNPAID)
                .paymentExpiresAt(LocalDateTime.now().plusMinutes(20))
                .bookingPrice(1000L)
                .platformBookingFee(1000L)
                .sourceRecurringBookingId(UUID.randomUUID())
                .build();
        Booking confirmed = Booking.builder()
                .id(bookingId)
                .clientId(userId)
                .status(BookingStatus.CONFIRMED)
                .paymentStatus(BookingPaymentStatus.PAID)
                .build();
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking))
                .thenReturn(Optional.of(booking))
                .thenReturn(Optional.of(confirmed));
        when(userBalanceClient.deduct(any())).thenReturn(new BalanceDeductionResponse(true, 0L, "Balance deducted"));
        when(bookingRepository.confirmPendingBookingFromPayment(bookingId, BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingPaymentStatus.PAID))
                .thenReturn(1);
        when(bookingMapper.toResponse(confirmed)).thenReturn(BookingResponse.builder().status(BookingStatus.CONFIRMED).build());

        BookingResponse response = bookingService.payPendingBooking(userId, bookingId);

        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        verify(bookingNotificationEventPublisher).publishRecurringPaymentWalletEmpty(booking);
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
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(request.getBookingDate().getDayOfWeek())))
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
    void createBookingRetriesWalletDeductionWhenBalanceProjectionLags() {
        ReflectionTestUtils.setField(bookingService, "balanceDeductionAttempts", 4);
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        SubFieldResponse subField = activeSubField(subFieldId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(60)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(request.getBookingDate().getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(request.getBookingDate()),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(9, 30)), anyCollection())).thenReturn(false);
        when(pricingStrategy.calculate(eq(subField), eq(request))).thenReturn(new BigDecimal("100000"));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userBalanceClient.deduct(any()))
                .thenReturn(new BalanceDeductionResponse(false, 0L, "Insufficient account balance"))
                .thenReturn(new BalanceDeductionResponse(false, 0L, "Insufficient account balance"))
                .thenReturn(new BalanceDeductionResponse(true, 9000L, "Balance deducted"));
        when(bookingRepository.confirmPendingBookingFromPayment(any(), eq(BookingStatus.PENDING), eq(BookingStatus.CONFIRMED), eq(BookingPaymentStatus.PAID)))
                .thenReturn(1);
        when(bookingRepository.findById(org.mockito.ArgumentMatchers.nullable(UUID.class)))
                .thenAnswer(invocation -> Optional.of(Booking.builder()
                        .id(invocation.getArgument(0))
                        .subFieldId(subFieldId)
                        .bookingDate(request.getBookingDate())
                        .build()));
        when(bookingMapper.toResponse(any(Booking.class), eq(subField))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            return BookingResponse.builder().status(booking.getStatus()).build();
        });

        BookingResponse response = bookingService.createBooking(userId, request);

        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        verify(userBalanceClient, times(3)).deduct(any());
        verify(bookingRepository).confirmPendingBookingFromPayment(any(), eq(BookingStatus.PENDING), eq(BookingStatus.CONFIRMED), eq(BookingPaymentStatus.PAID));
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
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(request.getBookingDate().getDayOfWeek())))
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
    void createBookingRejectsExistingPendingBeforeResolvingBookingFee() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        SubFieldResponse subField = activeSubField(subFieldId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(60)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(request.getBookingDate().getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(request.getBookingDate()),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(9, 30)), anyCollection())).thenReturn(false);
        when(bookingRepository.existsByClientIdAndStatus(userId, BookingStatus.PENDING)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> bookingService.createBooking(userId, request));

        verify(userProjectionRepository, never()).findById(userId);
        verify(bookingRepository, never()).saveAndFlush(any());
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
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(request.getBookingDate().getDayOfWeek())))
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
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(bookingDate.getDayOfWeek())))
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
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(bookingDate.getDayOfWeek())))
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
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(DayOfWeek.SUNDAY)))
                .thenReturn(new ResolvedOperatingHours(LocalTime.of(6, 0), LocalTime.of(23, 59), false));
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(DayOfWeek.MONDAY)))
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

        assertThrows(BadRequestException.class, () -> bookingService.createBooking(UUID.randomUUID(), request));
        verify(subFieldProjectionService, never()).resolveOperatingHours(any(), any(), any());
        verify(bookingRepository, never()).existsConflictingBookings(any(), any(), any(), any(), anyCollection());
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void createBookingAllowsConfiguredMaximumFutureBookingDate() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate bookingDate = LocalDate.now().plusDays(30);
        SubFieldResponse subField = activeSubField(subFieldId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(bookingDate)
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(60)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(bookingDate.getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(bookingDate),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(9, 30)), anyCollection())).thenReturn(false);
        when(pricingStrategy.calculate(eq(subField), eq(request))).thenReturn(new BigDecimal("100000"));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingMapper.toResponse(any(Booking.class), eq(subField))).thenReturn(BookingResponse.builder().build());

        bookingService.createBooking(userId, request);

        verify(bookingRepository).saveAndFlush(any(Booking.class));
    }

    @Test
    void createBookingRejectsDateBeyondConfiguredFutureBookingLimit() {
        ReflectionTestUtils.setField(bookingService, "maxBookingDaysInFuture", 7);
        UUID subFieldId = UUID.randomUUID();
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(8))
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(60)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(activeSubField(subFieldId));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> bookingService.createBooking(UUID.randomUUID(), request));

        assertEquals("Booking date cannot be more than 7 days in the future", exception.getMessage());
        assertEquals("BOOKING_DATE_OUT_OF_RANGE", exception.getCode());
        verify(subFieldProjectionService, never()).resolveOperatingHours(any(), any(), any());
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void validateRecurringOccurrenceDoesNotUseNormalFutureBookingLimit() {
        ReflectionTestUtils.setField(bookingService, "maxBookingDaysInFuture", 7);
        UUID userId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate bookingDate = LocalDate.now().plusDays(60);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(bookingDate)
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(60)
                .build();
        SubFieldResponse subField = activeSubField(subFieldId);

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(bookingDate.getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(bookingDate),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(9, 30)), anyCollection(), eq(recurringId))).thenReturn(false);

        bookingService.validateRecurringOccurrence(userId, request, recurringId);

        verify(subFieldProjectionService).resolveOperatingHours(eq(subFieldId), any(), eq(bookingDate.getDayOfWeek()));
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
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(request.getBookingDate().getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(request.getBookingDate()),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(9, 30)), anyCollection())).thenReturn(true);

        assertThrows(BookingConflictException.class, () -> bookingService.createBooking(UUID.randomUUID(), request));
        verify(bookingRepository, never()).saveAndFlush(any());
    }

    @Test
    void createBookingSkipsRecurringConflictQueryWhenSubFieldHasNoRecurringBooking() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        SubFieldResponse subField = activeSubField(subFieldId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(60)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(request.getBookingDate().getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(request.getBookingDate()),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(9, 30)), anyCollection())).thenReturn(false);
        when(pricingStrategy.calculate(eq(subField), eq(request))).thenReturn(new BigDecimal("100000"));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingMapper.toResponse(any(Booking.class), eq(subField))).thenReturn(BookingResponse.builder().build());

        bookingService.createBooking(userId, request);

        verify(recurringBookingRepository, never()).findActiveConflictsForDate(any(), any(), any(), any(), any(), any());
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
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(request.getBookingDate().getDayOfWeek())))
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
    void createBookingMapsDatabaseOverlapFromSameClientToAlreadyBookedMessage() {
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
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(request.getBookingDate().getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(request.getBookingDate()),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(10, 0)), anyCollection())).thenReturn(false);
        when(pricingStrategy.calculate(eq(subField), eq(request))).thenReturn(new BigDecimal("150000"));
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenThrow(new DataIntegrityViolationException(
                "violates exclusion constraint \"bookings_no_overlapping_active_bookings\""));
        when(bookingRepository.existsByClientIdAndSubFieldIdAndStartDateTimeAndEndDateTimeAndStatusIn(
                eq(userId),
                eq(subFieldId),
                eq(LocalDateTime.of(request.getBookingDate(), LocalTime.of(8, 30))),
                eq(LocalDateTime.of(request.getBookingDate(), LocalTime.of(10, 0))),
                anyCollection())).thenReturn(true);

        BookingConflictException exception = assertThrows(BookingConflictException.class,
                () -> bookingService.createBooking(userId, request));

        assertEquals("You have already booked this field successfully.", exception.getMessage());
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
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), any()))
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
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), any()))
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
    void getAvailabilityMarksClosureDatesClosed() {
        UUID subFieldId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);
        SubFieldResponse subField = activeSubField(subFieldId);

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), any()))
                .thenReturn(openHours());
        when(fieldClosureProjectionRepository.findOverlappingDateRange(
                eq(subFieldId), eq(date.minusDays(1)), eq(date.plusDays(7))))
                .thenReturn(List.of(SubFieldClosureProjection.builder()
                        .id(UUID.randomUUID())
                        .subFieldId(subFieldId)
                        .startDate(date)
                        .endDate(date.plusDays(1))
                        .build()));
        when(bookingRepository.findOverlappingBookings(
                eq(subFieldId), any(LocalDateTime.class), any(LocalDateTime.class), anyCollection()))
                .thenReturn(List.of());

        AvailabilityResponse response = bookingService.getAvailability(subFieldId, date);

        assertEquals(null, response.getOpenTime());
        assertEquals(null, response.getCloseTime());
        assertEquals(false, response.getOpen24Hours());
        assertTrue(response.getOperatingHours().stream()
                .filter(hours -> date.equals(hours.getDate()) || date.plusDays(1).equals(hours.getDate()))
                .allMatch(hours -> Boolean.TRUE.equals(hours.getClosed())
                        && hours.getOpenTime() == null
                        && hours.getCloseTime() == null
                        && !Boolean.TRUE.equals(hours.getOpen24Hours())));
    }

    @Test
    void createBookingOnClosureDateReturnsSpecificErrorCode() {
        UUID subFieldId = UUID.randomUUID();
        LocalDate bookingDate = LocalDate.now().plusDays(1);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(bookingDate)
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(60)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(activeSubField(subFieldId));
        when(fieldClosureProjectionRepository.existsOverlappingDateRange(subFieldId, bookingDate, bookingDate))
                .thenReturn(true);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> bookingService.createBooking(UUID.randomUUID(), request));

        assertEquals("SUBFIELD_CLOSED", exception.getCode());
        verify(subFieldProjectionService, never()).resolveOperatingHours(any(), any(), any());
        verify(bookingRepository, never()).saveAndFlush(any());
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
    void expirePendingBookingsPausesRecurringBookingAfterPaymentTimeout() {
        UUID bookingId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        Booking expiringBooking = Booking.builder()
                .id(bookingId)
                .subFieldId(subFieldId)
                .sourceRecurringBookingId(recurringId)
                .status(BookingStatus.PENDING)
                .paymentStatus(BookingPaymentStatus.UNPAID)
                .build();
        Booking expiredBooking = Booking.builder()
                .id(bookingId)
                .subFieldId(subFieldId)
                .sourceRecurringBookingId(recurringId)
                .status(BookingStatus.EXPIRED)
                .paymentStatus(BookingPaymentStatus.UNPAID)
                .build();

        when(bookingRepository.findPendingBookingsExpiringAtOrBefore(eq(BookingStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(expiringBooking));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(expiredBooking));
        when(bookingRepository.expirePendingBookings(
                eq(BookingStatus.PENDING),
                eq(BookingStatus.EXPIRED),
                any(LocalDateTime.class),
                eq("Payment timeout"),
                any(LocalDateTime.class),
                eq(BookingCancelledBy.SYSTEM),
                eq(BookingPaymentStatus.PAID),
                eq(BookingPaymentStatus.REFUNDED),
                eq(BookingPaymentStatus.FAILED))).thenReturn(1);
        when(recurringBookingRepository.updateStatus(
                recurringId,
                RecurringBookingStatus.ACTIVE,
                RecurringBookingStatus.PAUSED)).thenReturn(1);
        when(recurringBookingRepository.existsBySubFieldIdAndStatus(subFieldId, RecurringBookingStatus.ACTIVE))
                .thenReturn(false);

        int expiredCount = bookingService.expirePendingBookings();

        assertEquals(1, expiredCount);
        verify(recurringBookingRepository).updateStatus(recurringId, RecurringBookingStatus.ACTIVE, RecurringBookingStatus.PAUSED);
        verify(pendingBookingReservationService).release(expiredBooking);
        verify(bookingSubFieldProjectionRepository).updateHasRecurring(subFieldId, false);
        verify(bookingNotificationEventPublisher).publishRecurringPausedPaymentTimeout(expiredBooking);
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

        when(bookingRepository.findOwnerBookings(eq(ownerId), eq(null), eq(null), eq(null), eq(false), any(), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(booking), PageRequest.of(0, 10), 1));
        when(bookingMapper.toResponse(booking)).thenReturn(mapped);
        when(userProjectionRepository.findAllById(any())).thenReturn(List.of(UserProjection.builder()
                .userId(clientId)
                .fullName("Nguyen Van A")
                .phoneNumber("0862470050")
                .avatarUrl("https://example.com/avatar.png")
                .build()));
        when(matchResultRepository.findByBookingIdIn(List.of(bookingId))).thenReturn(List.of());

        var response = bookingService.getManagerBookings(ownerId, "OWNER", null, null, null, null, null, PageRequest.of(0, 10));

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
        verify(communityPostMaintenanceService, never()).cancelOpenPostForBooking(any());
        verify(bookingNotificationEventPublisher).publishBookingCancelled(cancelled, null);
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

    @Test
    void ownerCannotCreateNormalBookingOnOwnedField() {
        UUID ownerId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        SubFieldResponse subField = activeSubField(subFieldId);
        subField.setOwnerId(ownerId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(60)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);

        assertThrows(BadRequestException.class, () -> bookingService.createBooking(ownerId, request));
        verify(bookingRepository, never()).saveAndFlush(any());
        verify(userBalanceClient, never()).deduct(any());
    }

    @Test
    void ownerCreatesZeroCostReservationOnOwnedFieldWithoutPayment() {
        UUID ownerId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        SubFieldResponse subField = activeSubField(subFieldId);
        subField.setOwnerId(ownerId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(60)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(request.getBookingDate().getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(request.getBookingDate()),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(9, 30)), anyCollection())).thenReturn(false);
        when(bookingRepository.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingMapper.toResponse(any(Booking.class), eq(subField))).thenReturn(BookingResponse.builder()
                .bookingType(BookingType.RESERVATION)
                .subFieldPrice(BigDecimal.ZERO)
                .paymentStatus(BookingPaymentStatus.NOT_REQUIRED)
                .build());

        BookingResponse response = bookingService.createReservation(ownerId, request);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).saveAndFlush(bookingCaptor.capture());
        Booking reservation = bookingCaptor.getValue();
        assertEquals(BookingType.RESERVATION, reservation.getBookingType());
        assertEquals(BigDecimal.ZERO, reservation.getSubFieldPrice());
        assertEquals(0L, reservation.getBookingPrice());
        assertEquals(0L, reservation.getPlatformBookingFee());
        assertEquals(BookingStatus.CONFIRMED, reservation.getStatus());
        assertEquals(BookingPaymentStatus.NOT_REQUIRED, reservation.getPaymentStatus());
        assertEquals(BookingType.RESERVATION, response.getBookingType());
        verify(pricingStrategy, never()).calculate(any(), any());
        verify(userBalanceClient, never()).deduct(any());
        verify(bookingNotificationEventPublisher).publishReservationChanged(any(Booking.class), eq(subField), eq("CREATED"));
    }

    @Test
    void reservationUsesExistingConflictDetection() {
        UUID ownerId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        SubFieldResponse subField = activeSubField(subFieldId);
        subField.setOwnerId(ownerId);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 30))
                .durationMinutes(60)
                .build();

        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField);
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), any(), eq(request.getBookingDate().getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.existsConflictingBookings(eq(subFieldId), eq(request.getBookingDate()),
                eq(LocalTime.of(8, 30)), eq(LocalTime.of(9, 30)), anyCollection())).thenReturn(true);

        assertThrows(BookingConflictException.class, () -> bookingService.createReservation(ownerId, request));
        verify(bookingRepository, never()).saveAndFlush(any());
        verify(userBalanceClient, never()).deduct(any());
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

