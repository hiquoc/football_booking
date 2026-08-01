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
import com.project.booking.mapper.BookingMapper;
import com.project.booking.mapper.RecurringBookingMapper;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.BookingSubFieldProjectionRepository;
import com.project.booking.repository.RecurringBookingRepository;
import com.project.booking.service.BookingService;
import com.project.booking.service.SubFieldProjectionService;
import com.project.common.enums.BookingStatus;
import com.project.common.enums.RecurringBookingStatus;
import com.project.common.exception.BadRequestException;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private AvailabilityCacheService availabilityCacheService;

    @InjectMocks
    private RecurringBookingServiceImpl recurringBookingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recurringBookingService, "generationLeadDays", 0);
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
        when(recurringBookingRepository.findUserOverlapCandidates(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(recurringBookingRepository.findSubFieldOverlapCandidates(any(), any(), any(), any(), any(), any(), any()))
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
    void createRollsBackRuleWhenAnyOccurrenceConflicts() {
        UUID userId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().plusDays(1);
        CreateRecurringBookingRequest request = request(subFieldId, startDate, startDate.plusDays(4), 2);
        when(subFieldProjectionService.getRequiredSubField(subFieldId)).thenReturn(subField(subFieldId));
        when(bookingRepository.existsCompletedBookingAtField(eq(userId), any(), eq(BookingStatus.COMPLETED))).thenReturn(true);
        when(recurringBookingRepository.findUserOverlapCandidates(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(recurringBookingRepository.findSubFieldOverlapCandidates(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        org.mockito.Mockito.doThrow(new BookingConflictException("conflict"))
                .when(bookingService).validateRecurringOccurrence(eq(userId), any(), eq(null));

        assertThrows(BookingConflictException.class, () -> recurringBookingService.create(userId, request));

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
        when(recurringBookingRepository.findUserOverlapCandidates(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(recurringBookingRepository.findSubFieldOverlapCandidates(any(), any(), any(), any(), any(), any(), any()))
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
        assertEquals(startDate.plusDays(7).atStartOfDay(), recurringCaptor.getValue().getNextProcessAt());
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
