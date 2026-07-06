package com.project.booking.service.impl;

import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.response.AvailabilityResponse;
import com.project.booking.dto.response.BookingResponse;
import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.dto.response.TimePriceRuleDto;
import com.project.booking.entity.Booking;
import com.project.booking.exception.BookingConflictException;
import com.project.booking.kafka.BookingNotificationEventPublisher;
import com.project.booking.mapper.BookingMapper;
import com.project.booking.pricing.PricingStrategy;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.FieldClosureProjectionRepository;
import com.project.booking.service.ResolvedOperatingHours;
import com.project.booking.service.SubFieldProjectionService;
import com.project.common.enums.BookingCancelledBy;
import com.project.common.enums.BookingStatus;
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

    @InjectMocks
    private BookingServiceImpl bookingService;

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
    void confirmMockPaymentOnlyConfirmsPendingBookingForClient() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Booking pendingBooking = Booking.builder()
                .id(bookingId)
                .clientId(userId)
                .status(BookingStatus.PENDING)
                .build();
        Booking confirmedBooking = Booking.builder()
                .id(bookingId)
                .clientId(userId)
                .status(BookingStatus.CONFIRMED)
                .build();
        BookingResponse mappedResponse = BookingResponse.builder()
                .id(bookingId)
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(confirmedBooking));
        when(bookingRepository.confirmPendingBooking(
                bookingId,
                userId,
                BookingStatus.PENDING,
                BookingStatus.CONFIRMED)).thenReturn(1);
        when(bookingMapper.toResponse(confirmedBooking)).thenReturn(mappedResponse);

        BookingResponse response = bookingService.confirmMockPayment(userId, bookingId);

        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        verify(bookingRepository).confirmPendingBooking(
                bookingId,
                userId,
                BookingStatus.PENDING,
                BookingStatus.CONFIRMED);
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
        when(bookingRepository.completeConfirmedBookings(
                eq(BookingStatus.CONFIRMED),
                eq(BookingStatus.COMPLETED),
                any(LocalDate.class),
                any(LocalTime.class))).thenReturn(4);

        int completedCount = bookingService.completeFinishedBookings();

        assertEquals(4, completedCount);
        verify(bookingRepository).completeConfirmedBookings(
                eq(BookingStatus.CONFIRMED),
                eq(BookingStatus.COMPLETED),
                any(LocalDate.class),
                any(LocalTime.class));
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
