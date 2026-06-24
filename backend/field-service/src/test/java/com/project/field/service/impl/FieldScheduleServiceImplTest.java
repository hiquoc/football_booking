package com.project.field.service.impl;

import com.project.common.exception.BadRequestException;
import com.project.field.dto.FieldClosureRequest;
import com.project.field.dto.OperatingHoursRequest;
import com.project.field.entity.Field;
import com.project.field.entity.SubFieldClosure;
import com.project.field.entity.SubField;
import com.project.field.kafka.FieldEventPublisher;
import com.project.field.repository.FieldClosureRepository;
import com.project.field.repository.FieldOperatingHoursRepository;
import com.project.field.repository.FieldRepository;
import com.project.field.repository.SubFieldOperatingHoursRepository;
import com.project.field.repository.SubFieldRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
        verify(fieldEventPublisher).publishFieldOperatingHoursUpdated(anyList());
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
}
