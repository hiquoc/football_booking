package com.project.booking.service.impl;

import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.request.CancelBookingRequest;
import com.project.booking.dto.response.AvailabilityResponse;
import com.project.booking.dto.response.BookingResponse;
import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.dto.response.TimePriceRuleDto;
import com.project.booking.cache.AvailabilityCacheService;
import com.project.booking.community.service.CommunityPostMaintenanceService;
import com.project.booking.entity.Booking;
import com.project.booking.exception.BookingConflictException;
import com.project.booking.kafka.BookingNotificationEventPublisher;
import com.project.booking.kafka.BookingTrustEventPublisher;
import com.project.booking.mapper.BookingMapper;
import com.project.booking.moderation.service.BookingModerationService;
import com.project.booking.entity.BookingConfig;
import com.project.booking.kafka.BookingBalanceEventPublisher;
import com.project.booking.payment.BookingPaymentStrategy;
import com.project.booking.payment.BookingPaymentStrategyFactory;
import com.project.booking.pricing.PricingStrategy;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.FieldClosureProjectionRepository;
import com.project.booking.repository.RecurringBookingRepository;
import com.project.booking.repository.UserReplicaRepository;
import com.project.booking.entity.UserReplica;
import com.project.booking.service.ResolvedOperatingHours;
import com.project.booking.service.BookingConfigService;
import com.project.booking.service.SubFieldProjectionService;
import com.project.common.enums.BookingCancelledBy;
import com.project.common.enums.BookingStatus;
import com.project.common.enums.PaymentMethod;
import com.project.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
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
    private BookingPaymentStrategyFactory paymentStrategyFactory;

    @Mock
    private BookingBalanceEventPublisher bookingBalanceEventPublisher;

    @Mock
    private BookingPaymentStrategy bookingPaymentStrategy;

    @Mock
    private AvailabilityCacheService availabilityCacheService;

    @Mock
    private RecurringBookingRepository recurringBookingRepository;

    @Mock
    private CommunityPostMaintenanceService communityPostMaintenanceService;

    @Mock
    private UserReplicaRepository userReplicaRepository;

    @Mock
    private BookingTrustEventPublisher bookingTrustEventPublisher;

    @Mock
    private BookingModerationService bookingModerationService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(bookingConfigService.getConfig()).thenReturn(BookingConfig.builder()
                .firstBookingFee(5000L)
                .notFirstBookingFee(1000L)
                .refundBeforeHours(24)
                .refundEnabled(true)
                .build());
        org.mockito.Mockito.lenient().when(paymentStrategyFactory.get(org.mockito.ArgumentMatchers.any()))
                .thenReturn(bookingPaymentStrategy);
        org.mockito.Mockito.lenient().when(bookingPaymentStrategy.method()).thenReturn(PaymentMethod.STRIPE);
        org.mockito.Mockito.lenient().when(recurringBookingRepository.findActiveConflictsForDate(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(userReplicaRepository.findById(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());
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
    void createBookingStoresConfiguredFeeSeparatelyAndUsesRequestedPaymentStrategy() {
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
        verify(paymentStrategyFactory).get(PaymentMethod.ACCOUNT_BALANCE);
        verify(bookingPaymentStrategy).onBookingCreated(savedBooking, subField);
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

        when(userReplicaRepository.findById(userId)).thenReturn(Optional.of(UserReplica.builder()
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
        when(subFieldProjectionService.resolveOperatingHours(eq(subFieldId), eq(date.getDayOfWeek())))
                .thenReturn(openHours());
        when(bookingRepository.findBySubFieldIdAndBookingDateAndStatusInOrderByStartTimeAsc(
                eq(subFieldId), eq(date), anyCollection())).thenReturn(List.of(firstBooking, secondBooking));

        AvailabilityResponse response = bookingService.getAvailability(subFieldId, date);

        assertEquals(LocalTime.of(6, 0), response.getOpenTime());
        assertEquals(LocalTime.of(23, 0), response.getCloseTime());
        assertEquals(2, response.getUnavailableSlots().size());
        assertEquals(LocalTime.of(9, 0), response.getUnavailableSlots().get(0).getStartTime());
        assertEquals(LocalTime.of(10, 30), response.getUnavailableSlots().get(0).getEndTime());
        assertEquals(LocalTime.of(14, 0), response.getUnavailableSlots().get(1).getStartTime());
        assertEquals(LocalTime.of(16, 0), response.getUnavailableSlots().get(1).getEndTime());
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
                eq(BookingCancelledBy.SYSTEM))).thenReturn(3);

        int expiredCount = bookingService.expirePendingBookings();

        assertEquals(3, expiredCount);
        long timeoutMinutes = java.time.Duration.between(expiresBeforeCaptor.getValue(), cancelledAtCaptor.getValue())
                .toMinutes();
        assertEquals(20, timeoutMinutes);
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
                .build();

        when(bookingConfigService.getConfig()).thenReturn(BookingConfig.builder()
                .firstBookingFee(5000L)
                .notFirstBookingFee(1000L)
                .refundBeforeHours(24)
                .refundEnabled(true)
                .build());
        when(bookingRepository.cancelClientBooking(
                eq(bookingId), eq(userId), anyCollection(), eq(BookingStatus.CANCELLED),
                eq("Change of plans"), any(LocalDateTime.class), eq(BookingCancelledBy.CLIENT))).thenReturn(1);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(cancelled));
        when(bookingMapper.toResponse(cancelled)).thenReturn(BookingResponse.builder().build());

        bookingService.cancelBooking(userId, request);

        verify(bookingBalanceEventPublisher).publishRefundRequested(cancelled, 2000L, "BOOKING_CANCEL_REFUND");
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
                .build();

        when(bookingConfigService.getConfig()).thenReturn(BookingConfig.builder()
                .firstBookingFee(5000L)
                .notFirstBookingFee(1000L)
                .refundBeforeHours(48)
                .refundEnabled(true)
                .build());
        when(bookingRepository.cancelClientBooking(
                eq(bookingId), eq(userId), anyCollection(), eq(BookingStatus.CANCELLED),
                eq("Late cancellation"), any(LocalDateTime.class), eq(BookingCancelledBy.CLIENT))).thenReturn(1);
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

    private ResolvedOperatingHours openHours() {
        return new ResolvedOperatingHours(LocalTime.of(6, 0), LocalTime.of(23, 0), false);
    }
}
