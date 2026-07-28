package com.project.field.service.impl;

import com.project.common.exception.BadRequestException;
import com.project.common.dto.ApiResponse;
import com.project.field.client.BookingServiceClient;
import com.project.field.dto.FieldClosureRequest;
import com.project.field.dto.OperatingHoursRequest;
import com.project.field.entity.Field;
import com.project.field.entity.FieldOperatingHours;
import com.project.field.entity.SubFieldClosure;
import com.project.field.entity.SubField;
import com.project.field.entity.TimePriceRule;
import com.project.field.kafka.FieldEventPublisher;
import com.project.field.repository.FieldClosureRepository;
import com.project.field.repository.FieldOperatingHoursRepository;
import com.project.field.repository.FieldRepository;
import com.project.field.repository.SubFieldOperatingHoursRepository;
import com.project.field.repository.SubFieldRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FieldScheduleServiceImplTest {

    @Mock
    private FieldRepository fieldRepository;

    @Mock
    private SubFieldRepository subFieldRepository;

    @Mock
    private FieldOperatingHoursRepository fieldOperatingHoursRepository;

    @Mock
    private SubFieldOperatingHoursRepository subFieldOperatingHoursRepository;

    @Mock
    private FieldClosureRepository fieldClosureRepository;

    @Mock
    private FieldEventPublisher fieldEventPublisher;

    @Mock
    private BookingServiceClient bookingServiceClient;

    @Spy
    private OperatingHoursPriceRuleSynchronizer operatingHoursPriceRuleSynchronizer;

    @InjectMocks
    private FieldScheduleServiceImpl service;

    @Test
    void replaceFieldOperatingHoursRequiresAllSevenDays() {
        UUID fieldId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Field field = Field.builder().id(fieldId).ownerId(ownerId).build();
        when(fieldRepository.findById(fieldId)).thenReturn(Optional.of(field));

        List<OperatingHoursRequest> request = List.of(OperatingHoursRequest.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .openTime(LocalTime.of(6, 0))
                .closeTime(LocalTime.of(22, 0))
                .closed(false)
                .build());

        assertThrows(BadRequestException.class,
                () -> service.replaceFieldOperatingHours(fieldId, ownerId, "OWNER", request));
    }

    @Test
    void replaceFieldOperatingHoursPersistsAndPublishesEachDay() {
        UUID fieldId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Field field = Field.builder().id(fieldId).ownerId(ownerId).build();
        when(fieldRepository.findById(fieldId)).thenReturn(Optional.of(field));
        when(fieldOperatingHoursRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        service.replaceFieldOperatingHours(fieldId, ownerId, "OWNER", openWeek());

        verify(fieldOperatingHoursRepository).saveAll(anyList());
        verify(fieldEventPublisher).publishFieldOperatingHoursUpdated(anyList(), anyList(), eq(List.of()));
    }

    @Test
    void replaceFieldOperatingHoursAcceptsMidnightAndEndOfDayTimes() {
        UUID fieldId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Field field = Field.builder().id(fieldId).ownerId(ownerId).build();
        when(fieldRepository.findById(fieldId)).thenReturn(Optional.of(field));
        when(fieldOperatingHoursRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        service.replaceFieldOperatingHours(fieldId, ownerId, "OWNER", weekWith(DayOfWeek.MONDAY,
                OperatingHoursRequest.builder()
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .openTime(LocalTime.MIDNIGHT)
                        .closeTime(LocalTime.of(23, 59))
                        .closed(false)
                        .build()));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(fieldOperatingHoursRepository).saveAll(captor.capture());
        FieldOperatingHours savedMonday = (FieldOperatingHours) captor.getValue().stream()
                .filter(hours -> ((FieldOperatingHours) hours).getDayOfWeek() == DayOfWeek.MONDAY)
                .findFirst()
                .orElseThrow();
        assertEquals(LocalTime.MIDNIGHT, savedMonday.getOpenTime());
        assertEquals(LocalTime.of(23, 59), savedMonday.getCloseTime());
        assertEquals(false, savedMonday.getOpen24Hours());
    }

    @Test
    void replaceFieldOperatingHoursPersistsOpenAllDayWithoutTimes() {
        UUID fieldId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Field field = Field.builder().id(fieldId).ownerId(ownerId).build();
        when(fieldRepository.findById(fieldId)).thenReturn(Optional.of(field));
        when(fieldOperatingHoursRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        service.replaceFieldOperatingHours(fieldId, ownerId, "OWNER", weekWith(DayOfWeek.MONDAY,
                OperatingHoursRequest.builder()
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .closed(false)
                        .open24Hours(true)
                        .build()));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(fieldOperatingHoursRepository).saveAll(captor.capture());
        FieldOperatingHours savedMonday = (FieldOperatingHours) captor.getValue().stream()
                .filter(hours -> ((FieldOperatingHours) hours).getDayOfWeek() == DayOfWeek.MONDAY)
                .findFirst()
                .orElseThrow();
        assertEquals(true, savedMonday.getOpen24Hours());
        assertNull(savedMonday.getOpenTime());
        assertNull(savedMonday.getCloseTime());
    }

    @Test
    void replaceFieldOperatingHoursAcceptsCrossMidnightSchedules() {
        UUID fieldId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Field field = Field.builder().id(fieldId).ownerId(ownerId).build();
        when(fieldRepository.findById(fieldId)).thenReturn(Optional.of(field));
        when(fieldOperatingHoursRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        service.replaceFieldOperatingHours(fieldId, ownerId, "OWNER", weekWith(DayOfWeek.MONDAY,
                OperatingHoursRequest.builder()
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .openTime(LocalTime.of(18, 0))
                        .closeTime(LocalTime.of(2, 0))
                        .closed(false)
                        .build()));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(fieldOperatingHoursRepository).saveAll(captor.capture());
        FieldOperatingHours savedMonday = (FieldOperatingHours) captor.getValue().stream()
                .filter(hours -> ((FieldOperatingHours) hours).getDayOfWeek() == DayOfWeek.MONDAY)
                .findFirst()
                .orElseThrow();
        assertEquals(LocalTime.of(18, 0), savedMonday.getOpenTime());
        assertEquals(LocalTime.of(2, 0), savedMonday.getCloseTime());
    }

    @Test
    void replaceFieldOperatingHoursFillsExpandedEndOfDayPriceRulesForEverySubField() {
        UUID fieldId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Field field = Field.builder().id(fieldId).ownerId(ownerId).build();
        SubField subField = subFieldWithRules(field,
                rule(LocalTime.of(6, 0), LocalTime.of(21, 0), "200000"),
                rule(LocalTime.of(21, 0), LocalTime.of(23, 0), "300000"));
        when(fieldRepository.findById(fieldId)).thenReturn(Optional.of(field));
        when(fieldOperatingHoursRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(subFieldRepository.findByFieldId(fieldId)).thenReturn(List.of(subField));

        service.replaceFieldOperatingHours(fieldId, ownerId, "OWNER", weekWith(DayOfWeek.MONDAY,
                OperatingHoursRequest.builder()
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .openTime(LocalTime.of(6, 0))
                        .closeTime(LocalTime.of(23, 59))
                        .closed(false)
                        .build()));

        assertEquals(2, subField.getTimePriceRules().size());
        TimePriceRule extended = subField.getTimePriceRules().get(1);
        assertEquals(LocalTime.of(21, 0), extended.getStartTime());
        assertEquals(LocalTime.of(23, 59), extended.getEndTime());
        assertEquals(new BigDecimal("300000"), extended.getHourlyPrice());
        verify(fieldEventPublisher).publishTimePriceRulesChanged(subField);
    }

    @Test
    void replaceFieldOperatingHoursExtendsOpeningPriceRuleWhenOpeningEarlier() {
        UUID fieldId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Field field = Field.builder().id(fieldId).ownerId(ownerId).build();
        SubField subField = subFieldWithRules(field,
                rule(LocalTime.of(8, 0), LocalTime.of(17, 0), "150000"),
                rule(LocalTime.of(17, 0), LocalTime.of(23, 59), "170000"));
        when(fieldRepository.findById(fieldId)).thenReturn(Optional.of(field));
        when(fieldOperatingHoursRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(subFieldRepository.findByFieldId(fieldId)).thenReturn(List.of(subField));

        service.replaceFieldOperatingHours(fieldId, ownerId, "OWNER", weekWith(DayOfWeek.MONDAY,
                OperatingHoursRequest.builder()
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .openTime(LocalTime.of(6, 0))
                        .closeTime(LocalTime.of(23, 59))
                        .closed(false)
                        .build()));

        assertEquals(2, subField.getTimePriceRules().size());
        assertEquals(LocalTime.of(6, 0), subField.getTimePriceRules().get(0).getStartTime());
        assertEquals(LocalTime.of(17, 0), subField.getTimePriceRules().get(0).getEndTime());
        assertEquals(new BigDecimal("150000"), subField.getTimePriceRules().get(0).getHourlyPrice());
        verify(fieldEventPublisher).publishTimePriceRulesChanged(subField);
    }

    @Test
    void replaceFieldOperatingHoursFillsOpenAllDayGapsWithoutDeletingExistingRules() {
        UUID fieldId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Field field = Field.builder().id(fieldId).ownerId(ownerId).build();
        SubField subField = subFieldWithRules(field,
                rule(LocalTime.of(6, 0), LocalTime.of(21, 0), "200000"),
                rule(LocalTime.of(21, 0), LocalTime.of(23, 0), "300000"));
        when(fieldRepository.findById(fieldId)).thenReturn(Optional.of(field));
        when(fieldOperatingHoursRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(subFieldRepository.findByFieldId(fieldId)).thenReturn(List.of(subField));

        service.replaceFieldOperatingHours(fieldId, ownerId, "OWNER", weekWith(DayOfWeek.MONDAY,
                OperatingHoursRequest.builder()
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .closed(false)
                        .open24Hours(true)
                        .build()));

        assertEquals(2, subField.getTimePriceRules().size());
        assertEquals(LocalTime.MIDNIGHT, subField.getTimePriceRules().get(0).getStartTime());
        assertEquals(LocalTime.of(21, 0), subField.getTimePriceRules().get(0).getEndTime());
        assertEquals(LocalTime.of(21, 0), subField.getTimePriceRules().get(1).getStartTime());
        assertEquals(LocalTime.of(23, 59), subField.getTimePriceRules().get(1).getEndTime());
        assertEquals(new BigDecimal("200000"), subField.getTimePriceRules().get(0).getHourlyPrice());
        assertEquals(new BigDecimal("300000"), subField.getTimePriceRules().get(1).getHourlyPrice());
    }

    @Test
    void createClosureRejectsEndDateBeforeStartDate() {
        UUID subFieldId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        FieldClosureRequest request = FieldClosureRequest.builder()
                .subFieldIds(List.of(subFieldId))
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(1))
                .build();

        assertThrows(BadRequestException.class,
                () -> service.createClosures(ownerId, "OWNER", request));
    }

    @Test
    void createClosuresPersistsAndPublishesEachSelectedSubField() {
        UUID firstSubFieldId = UUID.randomUUID();
        UUID secondSubFieldId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Field field = Field.builder().id(UUID.randomUUID()).ownerId(ownerId).build();
        SubField firstSubField = SubField.builder().id(firstSubFieldId).field(field).build();
        SubField secondSubField = SubField.builder().id(secondSubFieldId).field(field).build();
        when(subFieldRepository.findWithFieldById(firstSubFieldId)).thenReturn(Optional.of(firstSubField));
        when(subFieldRepository.findWithFieldById(secondSubFieldId)).thenReturn(Optional.of(secondSubField));
        when(fieldClosureRepository.findOverlappingClosures(anySet(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(bookingServiceClient.hasBookingConflicts(anySet(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(ApiResponse.success(false));
        when(fieldClosureRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createClosures(ownerId, "OWNER", FieldClosureRequest.builder()
                .subFieldIds(List.of(firstSubFieldId, secondSubFieldId))
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .reason("Maintenance")
                .build());

        verify(fieldClosureRepository).saveAll(anyList());
        verify(fieldEventPublisher).publishClosureCreated(anyList());
    }

    @Test
    void createClosuresSkipsSubFieldsWithExistingOverlappingClosure() {
        UUID firstSubFieldId = UUID.randomUUID();
        UUID secondSubFieldId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Field field = Field.builder().id(UUID.randomUUID()).ownerId(ownerId).build();
        SubField firstSubField = SubField.builder().id(firstSubFieldId).field(field).build();
        SubField secondSubField = SubField.builder().id(secondSubFieldId).field(field).build();
        when(subFieldRepository.findWithFieldById(firstSubFieldId)).thenReturn(Optional.of(firstSubField));
        when(subFieldRepository.findWithFieldById(secondSubFieldId)).thenReturn(Optional.of(secondSubField));
        when(fieldClosureRepository.findOverlappingClosures(anySet(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(SubFieldClosure.builder()
                        .subFieldId(firstSubFieldId)
                        .startDate(LocalDate.now().plusDays(1))
                        .endDate(LocalDate.now().plusDays(2))
                        .build()));
        when(bookingServiceClient.hasBookingConflicts(anySet(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(ApiResponse.success(false));
        when(fieldClosureRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createClosures(ownerId, "OWNER", FieldClosureRequest.builder()
                .subFieldIds(List.of(firstSubFieldId, secondSubFieldId))
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .reason("Maintenance")
                .build());

        verify(fieldClosureRepository).saveAll(org.mockito.ArgumentMatchers.argThat(closures -> {
            int count = 0;
            for (SubFieldClosure ignored : closures) {
                count++;
            }
            return count == 1;
        }));
        verify(fieldEventPublisher).publishClosureCreated(anyList());
    }

    @Test
    void createClosuresRejectsAnySelectedSubFieldWithBooking() {
        UUID subFieldId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Field field = Field.builder().id(UUID.randomUUID()).ownerId(ownerId).build();
        when(subFieldRepository.findWithFieldById(subFieldId))
                .thenReturn(Optional.of(SubField.builder().id(subFieldId).field(field).build()));
        when(bookingServiceClient.hasBookingConflicts(anySet(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(ApiResponse.success(true));

        FieldClosureRequest request = FieldClosureRequest.builder()
                .subFieldIds(List.of(subFieldId))
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .reason("Maintenance")
                .build();

        assertThrows(BadRequestException.class,
                () -> service.createClosures(ownerId, "OWNER", request));
        verify(fieldClosureRepository, org.mockito.Mockito.never()).saveAll(anyList());
    }

    @Test
    void updateClosureRejectsBookedDateRange() {
        UUID closureId = UUID.randomUUID();
        UUID subFieldId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Field field = Field.builder().id(UUID.randomUUID()).ownerId(ownerId).build();
        SubFieldClosure closure = SubFieldClosure.builder().id(closureId).subFieldId(subFieldId).build();
        when(fieldClosureRepository.findById(closureId)).thenReturn(Optional.of(closure));
        when(subFieldRepository.findWithFieldById(subFieldId))
                .thenReturn(Optional.of(SubField.builder().id(subFieldId).field(field).build()));
        when(bookingServiceClient.hasBookingConflicts(anySet(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(ApiResponse.success(true));

        FieldClosureRequest request = FieldClosureRequest.builder()
                .subFieldIds(List.of(subFieldId))
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(2))
                .reason("Maintenance")
                .build();

        assertThrows(BadRequestException.class,
                () -> service.updateClosure(closureId, ownerId, "OWNER", request));
        verify(fieldClosureRepository, org.mockito.Mockito.never()).save(any(SubFieldClosure.class));
    }

    private List<OperatingHoursRequest> openWeek() {
        return Arrays.stream(DayOfWeek.values())
                .map(day -> OperatingHoursRequest.builder()
                        .dayOfWeek(day)
                        .openTime(LocalTime.of(6, 0))
                        .closeTime(LocalTime.of(22, 0))
                        .closed(false)
                        .build())
                .toList();
    }

    private List<OperatingHoursRequest> weekWith(DayOfWeek dayOfWeek, OperatingHoursRequest replacement) {
        return openWeek().stream()
                .map(hours -> hours.getDayOfWeek() == dayOfWeek ? replacement : hours)
                .toList();
    }

    private SubField subFieldWithRules(Field field, TimePriceRule... rules) {
        SubField subField = SubField.builder()
                .id(UUID.randomUUID())
                .field(field)
                .timePriceRules(new java.util.ArrayList<>())
                .build();
        for (TimePriceRule rule : rules) {
            rule.setSubField(subField);
            subField.getTimePriceRules().add(rule);
        }
        return subField;
    }

    private TimePriceRule rule(LocalTime startTime, LocalTime endTime, String price) {
        return TimePriceRule.builder()
                .startTime(startTime)
                .endTime(endTime)
                .hourlyPrice(new BigDecimal(price))
                .build();
    }
}
