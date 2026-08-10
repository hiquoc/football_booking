package com.project.booking.service.impl;

import com.project.booking.cache.AvailabilityCacheService;
import com.project.booking.dto.request.CancelBookingRequest;
import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.request.CreateRecurringBookingRequest;
import com.project.booking.dto.response.BookingResponse;
import com.project.booking.dto.response.RecurringBookingResponse;
import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.entity.Booking;
import com.project.booking.entity.RecurringBooking;
import com.project.booking.entity.SubFieldProjection;
import com.project.booking.exception.BookingConflictException;
import com.project.booking.kafka.RecurringBookingOccurrenceEventPublisher;
import com.project.booking.mapper.BookingMapper;
import com.project.booking.mapper.RecurringBookingMapper;
import com.project.booking.moderation.service.BookingModerationService;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.BookingSubFieldProjectionRepository;
import com.project.booking.repository.RecurringBookingRepository;
import com.project.booking.service.BookingService;
import com.project.booking.service.SubFieldProjectionService;
import com.project.common.enums.BookingStatus;
import com.project.common.enums.RecurringBookingStatus;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.ForbiddenException;
import com.project.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringBookingServiceImplTest {

    @Mock
    private RecurringBookingRepository recurringBookingRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingSubFieldProjectionRepository subFieldRepository;

    @Mock
    private SubFieldProjectionService subFieldProjectionService;

    @Mock
    private RecurringBookingMapper recurringBookingMapper;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private BookingService bookingService;

    @Mock
    private BookingModerationService bookingModerationService;

    @Mock
    private RecurringBookingOccurrenceEventPublisher occurrenceEventPublisher;

    @Mock
    private AvailabilityCacheService availabilityCacheService;

    @InjectMocks
    private RecurringBookingServiceImpl recurringBookingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recurringBookingService, "generationWindowDays", 7);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7})
    void createValidatesEveryGeneratedOccurrenceForSupportedIntervals(int intervalDays) {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);
        CreateRecurringBookingRequest request = request(subFieldId, startDate, startDate.plusDays(7), intervalDays);
        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField(subFieldId));
        when(bookingRepository.existsCompletedBookingAtField(eq(userId), any(), eq(BookingStatus.COMPLETED))).thenReturn(true);
        when(recurringBookingRepository.findUserOverlapCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(recurringBookingRepository.findSubFieldOverlapCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(recurringBookingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(recurringBookingMapper.toResponse(any())).thenReturn(RecurringBookingResponse.builder().build());

        recurringBookingService.create(userId, request);

        int expectedOccurrences = 1 + (int) (7 / intervalDays);
        ArgumentCaptor<CreateBookingRequest> occurrenceCaptor = ArgumentCaptor.forClass(CreateBookingRequest.class);
        verify(bookingService, org.mockito.Mockito.times(expectedOccurrences))
                .validateRecurringOccurrence(eq(userId), occurrenceCaptor.capture(), eq(null));
        assertEquals(startDate, occurrenceCaptor.getAllValues().get(0).getBookingDate());
        verify(recurringBookingRepository).save(any(RecurringBooking.class));
    }

    @Test
    void createRejectsInvalidIntervalBeforeSaving() {
        UUID subFieldId = UUID.randomUUID();
        CreateRecurringBookingRequest request = request(subFieldId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), 8);
        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField(subFieldId));

        assertThrows(BadRequestException.class, () -> recurringBookingService.create(UUID.randomUUID(), request));

        verify(recurringBookingRepository, never()).save(any());
    }

    @Test
    void createRejectsRecurringEndDateMoreThanOneYearAfterStartDate() {
        UUID subFieldId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);
        CreateRecurringBookingRequest request = request(subFieldId, startDate, startDate.plusYears(1).plusDays(1), 7);
        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField(subFieldId));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> recurringBookingService.create(UUID.randomUUID(), request));

        assertEquals("RECURRING_BOOKING_END_DATE_OUT_OF_RANGE", exception.getCode());
        verify(recurringBookingRepository, never()).save(any());
        verify(bookingService, never()).validateRecurringOccurrence(any(), any(), any());
    }

    @Test
    void createRejectsIneligibleUserWithRecurringCompletedBookingRequiredCode() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);
        CreateRecurringBookingRequest request = request(subFieldId, startDate, startDate.plusDays(7), 7);
        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField(subFieldId));
        when(bookingRepository.existsCompletedBookingAtField(eq(userId), any(), eq(BookingStatus.COMPLETED))).thenReturn(false);

        ForbiddenException exception = assertThrows(
                ForbiddenException.class,
                () -> recurringBookingService.create(userId, request));

        assertEquals("RECURRING_BOOKING_COMPLETED_BOOKING_REQUIRED", exception.getCode());
        verify(recurringBookingRepository, never()).save(any());
        verify(bookingService, never()).validateRecurringOccurrence(any(), any(), any());
    }

    @Test
    void createRollsBackRuleWhenAnyOccurrenceConflicts() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);
        CreateRecurringBookingRequest request = request(subFieldId, startDate, startDate.plusDays(4), 2);
        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField(subFieldId));
        when(bookingRepository.existsCompletedBookingAtField(eq(userId), any(), eq(BookingStatus.COMPLETED))).thenReturn(true);
        when(recurringBookingRepository.findUserOverlapCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(recurringBookingRepository.findSubFieldOverlapCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        org.mockito.Mockito.doThrow(new BookingConflictException("conflict"))
                .when(bookingService).validateRecurringOccurrence(eq(userId), any(), eq(null));

        assertThrows(BookingConflictException.class, () -> recurringBookingService.create(userId, request));

        verify(recurringBookingRepository, never()).save(any());
    }

    @Test
    void createReportsClosureDateWhenAnyOccurrenceFallsOnClosureDay() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate closureDate = startDate.plusDays(2);
        CreateRecurringBookingRequest request = request(subFieldId, startDate, startDate.plusDays(4), 2);
        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField(subFieldId));
        when(bookingRepository.existsCompletedBookingAtField(eq(userId), any(), eq(BookingStatus.COMPLETED))).thenReturn(true);
        when(recurringBookingRepository.findUserOverlapCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(recurringBookingRepository.findSubFieldOverlapCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        org.mockito.Mockito.doAnswer(invocation -> {
            CreateBookingRequest occurrence = invocation.getArgument(1);
            if (closureDate.equals(occurrence.getBookingDate())) {
                throw new BadRequestException("Sub-field is closed on the selected booking date", "SUBFIELD_CLOSED");
            }
            return null;
        }).when(bookingService).validateRecurringOccurrence(eq(userId), any(), eq(null));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> recurringBookingService.create(userId, request));

        assertEquals("RECURRING_SUBFIELD_CLOSED_ON_DATE", exception.getCode());
        assertEquals("Sân sẽ đóng vào ngày " + closureDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".", exception.getMessage());
        verify(recurringBookingRepository, never()).save(any());
    }

    @Test
    void createImmediatelyBooksFirstOccurrenceAndReturnsIt() {
        UUID userId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);
        CreateRecurringBookingRequest request = request(subFieldId, startDate, startDate.plusDays(14), 7);
        BookingResponse firstBooking = BookingResponse.builder().id(UUID.randomUUID()).build();
        RecurringBookingResponse mappedResponse = RecurringBookingResponse.builder().build();
        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField(subFieldId));
        when(bookingRepository.existsCompletedBookingAtField(eq(userId), any(), eq(BookingStatus.COMPLETED))).thenReturn(true);
        when(recurringBookingRepository.findUserOverlapCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(recurringBookingRepository.findSubFieldOverlapCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(recurringBookingRepository.save(any())).thenAnswer(invocation -> {
            RecurringBooking recurringBooking = invocation.getArgument(0);
            recurringBooking.setId(recurringId);
            return recurringBooking;
        });
        when(bookingService.createRecurringOccurrence(eq(userId), eq(recurringId), any())).thenReturn(firstBooking);
        when(recurringBookingMapper.toResponse(any())).thenReturn(mappedResponse);

        RecurringBookingResponse response = recurringBookingService.create(userId, request);

        assertSame(firstBooking, response.getFirstBooking());
        ArgumentCaptor<CreateBookingRequest> firstBookingCaptor = ArgumentCaptor.forClass(CreateBookingRequest.class);
        verify(bookingService).createRecurringOccurrence(eq(userId), eq(recurringId), firstBookingCaptor.capture());
        assertEquals(startDate, firstBookingCaptor.getValue().getBookingDate());
        ArgumentCaptor<RecurringBooking> recurringCaptor = ArgumentCaptor.forClass(RecurringBooking.class);
        verify(recurringBookingRepository).save(recurringCaptor.capture());
        assertNotNull(recurringCaptor.getValue().getNextProcessAt());
    }

    @Test
    void createAllowsRecurringBookingAcrossMidnight() {
        UUID userId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);
        CreateRecurringBookingRequest request = CreateRecurringBookingRequest.builder()
                .subFieldId(subFieldId)
                .startDate(startDate)
                .endDate(startDate.plusDays(7))
                .intervalDays(7)
                .startTime(LocalTime.of(23, 0))
                .endTime(LocalTime.of(1, 0))
                .build();
        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField(subFieldId));
        when(bookingRepository.existsCompletedBookingAtField(eq(userId), any(), eq(BookingStatus.COMPLETED))).thenReturn(true);
        when(recurringBookingRepository.findUserOverlapCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(recurringBookingRepository.findSubFieldOverlapCandidates(any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(recurringBookingRepository.save(any())).thenAnswer(invocation -> {
            RecurringBooking recurringBooking = invocation.getArgument(0);
            recurringBooking.setId(recurringId);
            return recurringBooking;
        });
        when(bookingService.createRecurringOccurrence(eq(userId), eq(recurringId), any()))
                .thenReturn(BookingResponse.builder().build());
        when(recurringBookingMapper.toResponse(any())).thenReturn(RecurringBookingResponse.builder().build());

        recurringBookingService.create(userId, request);

        ArgumentCaptor<CreateBookingRequest> occurrenceCaptor = ArgumentCaptor.forClass(CreateBookingRequest.class);
        verify(bookingService, org.mockito.Mockito.times(2))
                .validateRecurringOccurrence(eq(userId), occurrenceCaptor.capture(), eq(null));
        assertEquals(120, occurrenceCaptor.getAllValues().getFirst().getDurationMinutes());
        ArgumentCaptor<CreateBookingRequest> firstBookingCaptor = ArgumentCaptor.forClass(CreateBookingRequest.class);
        verify(bookingService).createRecurringOccurrence(eq(userId), eq(recurringId), firstBookingCaptor.capture());
        assertEquals(120, firstBookingCaptor.getValue().getDurationMinutes());
    }

    @Test
    void createReturnsExistingRuleAndFirstBookingForExactReplay() {
        UUID userId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);
        CreateRecurringBookingRequest request = request(subFieldId, startDate, startDate.plusDays(14), 7);
        RecurringBooking existing = RecurringBooking.builder()
                .id(recurringId)
                .userId(userId)
                .fieldId(UUID.randomUUID())
                .subFieldId(subFieldId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .intervalDays(request.getIntervalDays())
                .status(RecurringBookingStatus.ACTIVE)
                .nextProcessAt(startDate.plusDays(7).atStartOfDay())
                .build();
        Booking firstBookingEntity = Booking.builder().id(UUID.randomUUID()).build();
        BookingResponse firstBooking = BookingResponse.builder().id(firstBookingEntity.getId()).build();
        RecurringBookingResponse mappedResponse = RecurringBookingResponse.builder().build();
        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField(subFieldId));
        when(bookingRepository.existsCompletedBookingAtField(eq(userId), any(), eq(BookingStatus.COMPLETED))).thenReturn(true);
        when(recurringBookingRepository.findFirstByUserIdAndSubFieldIdAndStartTimeAndEndTimeAndStartDateAndEndDateAndIntervalDaysAndStatus(
                eq(userId), eq(subFieldId), eq(request.getStartTime()), eq(request.getEndTime()),
                eq(request.getStartDate()), eq(request.getEndDate()), eq(request.getIntervalDays()), eq(RecurringBookingStatus.ACTIVE)))
                .thenReturn(Optional.of(existing));
        when(bookingRepository.findFirstBySourceRecurringBookingIdOrderByStartDateTimeAsc(recurringId))
                .thenReturn(Optional.of(firstBookingEntity));
        when(bookingMapper.toResponse(firstBookingEntity)).thenReturn(firstBooking);
        when(recurringBookingMapper.toResponse(existing)).thenReturn(mappedResponse);

        RecurringBookingResponse response = recurringBookingService.create(userId, request);

        assertSame(firstBooking, response.getFirstBooking());
        verify(recurringBookingRepository, never()).save(any());
        verify(bookingService, never()).createRecurringOccurrence(any(), any(), any());
        verify(bookingService, never()).validateRecurringOccurrence(any(), any(), any());
    }

    @Test
    void ownerPauseAllowsFieldOwnerToPauseRecurringBooking() {
        UUID ownerId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        RecurringBooking recurringBooking = ownerRecurringBooking(recurringId, ownerId, subFieldId);
        RecurringBookingResponse mappedResponse = RecurringBookingResponse.builder().build();
        when(recurringBookingRepository.findById(recurringId)).thenReturn(Optional.of(recurringBooking));
        when(recurringBookingRepository.save(recurringBooking)).thenReturn(recurringBooking);
        when(recurringBookingMapper.toResponse(recurringBooking)).thenReturn(mappedResponse);
        when(recurringBookingRepository.existsBySubFieldIdAndStatus(subFieldId, RecurringBookingStatus.ACTIVE))
                .thenReturn(false);

        RecurringBookingResponse response = recurringBookingService.ownerPause(ownerId, recurringId);

        assertSame(mappedResponse, response);
        assertEquals(RecurringBookingStatus.PAUSED, recurringBooking.getStatus());
        verify(subFieldRepository).updateHasRecurring(subFieldId, false);
        verify(availabilityCacheService).evictAll();
    }

    @Test
    void ownerPauseRejectsNonOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        RecurringBooking recurringBooking = ownerRecurringBooking(recurringId, UUID.randomUUID(), UUID.randomUUID());
        when(recurringBookingRepository.findById(recurringId)).thenReturn(Optional.of(recurringBooking));

        assertThrows(UnauthorizedException.class, () -> recurringBookingService.ownerPause(ownerId, recurringId));

        verify(recurringBookingRepository, never()).save(any());
    }

    @Test
    void resumeActivatesRecurringBookingAndReturnsOccupiedDatesWhenSomeOccurrencesAreAvailable() {
        UUID userId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        LocalDate occupiedDate = today.plusDays(2);
        RecurringBooking recurringBooking = RecurringBooking.builder()
                .id(recurringId)
                .userId(userId)
                .fieldId(UUID.randomUUID())
                .subFieldId(subFieldId)
                .startDate(today)
                .endDate(today.plusDays(4))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .intervalDays(2)
                .status(RecurringBookingStatus.PAUSED)
                .build();
        RecurringBookingResponse mappedResponse = RecurringBookingResponse.builder().build();
        when(recurringBookingRepository.findById(recurringId)).thenReturn(Optional.of(recurringBooking));
        when(bookingRepository.findOverlappingBookings(eq(subFieldId), any(), any(), any()))
                .thenReturn(List.of(occupiedBooking(subFieldId, occupiedDate)));
        when(bookingRepository.existsBySourceRecurringBookingIdAndBookingDateAndStatusIn(eq(recurringId), any(), any()))
                .thenReturn(false);
        when(occurrenceEventPublisher.publishRequested(eq(recurringBooking), any(), eq(60))).thenReturn(true);
        when(recurringBookingRepository.save(recurringBooking)).thenReturn(recurringBooking);
        when(recurringBookingMapper.toResponse(recurringBooking)).thenReturn(mappedResponse);
        when(recurringBookingRepository.existsBySubFieldIdAndStatus(subFieldId, RecurringBookingStatus.ACTIVE))
                .thenReturn(true);

        RecurringBookingResponse response = recurringBookingService.resume(userId, recurringId);

        assertSame(mappedResponse, response);
        assertEquals(RecurringBookingStatus.ACTIVE, recurringBooking.getStatus());
        assertIterableEquals(List.of(today, today.plusDays(4)), response.getGeneratedDates());
        assertIterableEquals(List.of(occupiedDate), response.getOccupiedDates());
        verify(occurrenceEventPublisher, org.mockito.Mockito.times(2)).publishRequested(eq(recurringBooking), any(), eq(60));
        verify(availabilityCacheService).evictAll();
    }

    @Test
    void resumeKeepsRecurringBookingPausedWhenAllWindowOccurrencesAreOccupied() {
        UUID userId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        RecurringBooking recurringBooking = RecurringBooking.builder()
                .id(recurringId)
                .userId(userId)
                .fieldId(UUID.randomUUID())
                .subFieldId(subFieldId)
                .startDate(today)
                .endDate(today.plusDays(4))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .intervalDays(2)
                .status(RecurringBookingStatus.PAUSED)
                .build();
        RecurringBookingResponse mappedResponse = RecurringBookingResponse.builder().build();
        when(recurringBookingRepository.findById(recurringId)).thenReturn(Optional.of(recurringBooking));
        when(bookingRepository.findOverlappingBookings(eq(subFieldId), any(), any(), any()))
                .thenReturn(List.of(
                        occupiedBooking(subFieldId, today),
                        occupiedBooking(subFieldId, today.plusDays(2)),
                        occupiedBooking(subFieldId, today.plusDays(4))));
        when(recurringBookingRepository.save(recurringBooking)).thenReturn(recurringBooking);
        when(recurringBookingMapper.toResponse(recurringBooking)).thenReturn(mappedResponse);
        when(recurringBookingRepository.existsBySubFieldIdAndStatus(subFieldId, RecurringBookingStatus.ACTIVE))
                .thenReturn(false);

        RecurringBookingResponse response = recurringBookingService.resume(userId, recurringId);

        assertSame(mappedResponse, response);
        assertEquals(RecurringBookingStatus.PAUSED, recurringBooking.getStatus());
        assertIterableEquals(List.of(), response.getGeneratedDates());
        assertIterableEquals(List.of(today, today.plusDays(2), today.plusDays(4)), response.getOccupiedDates());
        verify(occurrenceEventPublisher, never()).publishRequested(any(), any(), any(Integer.class));
        verify(availabilityCacheService).evictAll();
    }

    @Test
    void resumeSkipsAlreadyGeneratedOccurrencesWithoutReturningThemAsGenerated() {
        UUID userId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        RecurringBooking recurringBooking = RecurringBooking.builder()
                .id(recurringId)
                .userId(userId)
                .fieldId(UUID.randomUUID())
                .subFieldId(subFieldId)
                .startDate(today)
                .endDate(today.plusDays(2))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .intervalDays(2)
                .status(RecurringBookingStatus.PAUSED)
                .build();
        RecurringBookingResponse mappedResponse = RecurringBookingResponse.builder().build();
        when(recurringBookingRepository.findById(recurringId)).thenReturn(Optional.of(recurringBooking));
        when(bookingRepository.findOverlappingBookings(eq(subFieldId), any(), any(), any())).thenReturn(List.of());
        when(bookingRepository.existsBySourceRecurringBookingIdAndBookingDateAndStatusIn(
                eq(recurringId),
                eq(today),
                any())).thenReturn(true);
        when(bookingRepository.existsBySourceRecurringBookingIdAndBookingDateAndStatusIn(
                eq(recurringId),
                eq(today.plusDays(2)),
                any())).thenReturn(false);
        when(occurrenceEventPublisher.publishRequested(eq(recurringBooking), any(), eq(60))).thenReturn(true);
        when(recurringBookingRepository.save(recurringBooking)).thenReturn(recurringBooking);
        when(recurringBookingMapper.toResponse(recurringBooking)).thenReturn(mappedResponse);
        when(recurringBookingRepository.existsBySubFieldIdAndStatus(subFieldId, RecurringBookingStatus.ACTIVE))
                .thenReturn(true);

        RecurringBookingResponse response = recurringBookingService.resume(userId, recurringId);

        assertEquals(RecurringBookingStatus.ACTIVE, recurringBooking.getStatus());
        assertIterableEquals(List.of(today.plusDays(2)), response.getGeneratedDates());
        assertIterableEquals(List.of(), response.getOccupiedDates());
        verify(occurrenceEventPublisher, org.mockito.Mockito.times(1)).publishRequested(eq(recurringBooking), any(), eq(60));
    }

    @Test
    void resumeActivatesWhenWindowOccurrencesAlreadyExistForSameRecurringBooking() {
        UUID userId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate today = LocalDate.now();
        RecurringBooking recurringBooking = RecurringBooking.builder()
                .id(recurringId)
                .userId(userId)
                .fieldId(UUID.randomUUID())
                .subFieldId(subFieldId)
                .startDate(today)
                .endDate(today.plusDays(2))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .intervalDays(2)
                .status(RecurringBookingStatus.PAUSED)
                .build();
        RecurringBookingResponse mappedResponse = RecurringBookingResponse.builder().build();
        when(recurringBookingRepository.findById(recurringId)).thenReturn(Optional.of(recurringBooking));
        when(bookingRepository.findOverlappingBookings(eq(subFieldId), any(), any(), any())).thenReturn(List.of());
        when(bookingRepository.existsBySourceRecurringBookingIdAndBookingDateAndStatusIn(eq(recurringId), any(), any()))
                .thenReturn(true);
        when(recurringBookingRepository.save(recurringBooking)).thenReturn(recurringBooking);
        when(recurringBookingMapper.toResponse(recurringBooking)).thenReturn(mappedResponse);
        when(recurringBookingRepository.existsBySubFieldIdAndStatus(subFieldId, RecurringBookingStatus.ACTIVE))
                .thenReturn(true);

        RecurringBookingResponse response = recurringBookingService.resume(userId, recurringId);

        assertEquals(RecurringBookingStatus.ACTIVE, recurringBooking.getStatus());
        assertIterableEquals(List.of(), response.getGeneratedDates());
        assertIterableEquals(List.of(), response.getOccupiedDates());
        verify(occurrenceEventPublisher, never()).publishRequested(any(), any(), any(Integer.class));
    }

    @Test
    void ownerCancelCancelsLatestConfirmedBookingAsOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID recurringId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        UUID latestBookingId = UUID.randomUUID();
        RecurringBooking recurringBooking = ownerRecurringBooking(recurringId, ownerId, subFieldId);
        Booking latestBooking = Booking.builder()
                .id(latestBookingId)
                .status(BookingStatus.CONFIRMED)
                .build();
        when(recurringBookingRepository.findById(recurringId)).thenReturn(Optional.of(recurringBooking));
        when(bookingRepository.findFirstBySourceRecurringBookingIdOrderByStartDateTimeDesc(recurringId))
                .thenReturn(Optional.of(latestBooking));
        when(recurringBookingRepository.save(recurringBooking)).thenReturn(recurringBooking);
        when(recurringBookingMapper.toResponse(recurringBooking)).thenReturn(RecurringBookingResponse.builder().build());

        recurringBookingService.ownerCancel(ownerId, recurringId);

        ArgumentCaptor<CancelBookingRequest> requestCaptor = ArgumentCaptor.forClass(CancelBookingRequest.class);
        verify(bookingService).cancelBookingByManager(eq(ownerId), eq("OWNER"), requestCaptor.capture());
        assertEquals(latestBookingId, requestCaptor.getValue().getBookingId());
        assertEquals(RecurringBookingStatus.CANCELLED, recurringBooking.getStatus());
        verify(availabilityCacheService).evictAll();
    }

    private CreateRecurringBookingRequest request(UUID subFieldId, LocalDate startDate, LocalDate endDate, int intervalDays) {
        return CreateRecurringBookingRequest.builder()
                .subFieldId(subFieldId)
                .startDate(startDate)
                .endDate(endDate)
                .intervalDays(intervalDays)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .build();
    }

    private Booking occupiedBooking(UUID subFieldId, LocalDate bookingDate) {
        LocalDateTime startDateTime = LocalDateTime.of(bookingDate, LocalTime.of(8, 0));
        return Booking.builder()
                .id(UUID.randomUUID())
                .subFieldId(subFieldId)
                .sourceRecurringBookingId(UUID.randomUUID())
                .startDateTime(startDateTime)
                .endDateTime(startDateTime.plusHours(1))
                .status(BookingStatus.CONFIRMED)
                .build();
    }

    private SubFieldResponse subField(UUID subFieldId) {
        return SubFieldResponse.builder()
                .id(subFieldId)
                .fieldId(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .name("Sub-field")
                .fieldName("Field")
                .active(true)
                .status("ACTIVE")
                .build();
    }

    private RecurringBooking ownerRecurringBooking(UUID recurringId, UUID ownerId, UUID subFieldId) {
        SubFieldProjection subField = SubFieldProjection.builder()
                .id(subFieldId)
                .fieldId(UUID.randomUUID())
                .ownerId(ownerId)
                .name("Sub-field")
                .active(true)
                .build();
        return RecurringBooking.builder()
                .id(recurringId)
                .userId(UUID.randomUUID())
                .fieldId(subField.getFieldId())
                .subFieldId(subFieldId)
                .subField(subField)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(8))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .intervalDays(7)
                .status(RecurringBookingStatus.ACTIVE)
                .nextProcessAt(LocalDate.now().plusDays(8).atStartOfDay())
                .build();
    }
}
