package com.project.field.service.impl;

import com.project.common.enums.SportType;
import com.project.common.enums.SubFieldType;
import com.project.common.exception.BadRequestException;
import com.project.field.dto.SubFieldRequest;
import com.project.field.dto.BookingRuleDto;
import com.project.field.dto.TimePriceRuleDto;
import com.project.field.entity.BookingRule;
import com.project.field.entity.Field;
import com.project.field.entity.FieldOperatingHours;
import com.project.field.entity.FieldType;
import com.project.field.entity.SubField;
import com.project.field.kafka.FieldEventPublisher;
import com.project.field.mapper.SubFieldMapper;
import com.project.field.repository.FieldOperatingHoursRepository;
import com.project.field.repository.FieldRepository;
import com.project.field.repository.SubFieldRepository;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubFieldServiceImplTest {

    @Test
    void updateChangesOnlySuppliedFieldsIncludingNestedBookingRuleFields() {
        UUID subFieldId = UUID.randomUUID();
        FieldType football = FieldType.builder().id(1L).name(SportType.FOOTBALL).build();
        Field field = Field.builder().name("Sports Center").fieldTypes(Set.of(football)).build();
        BookingRule bookingRule = BookingRule.builder()
                .minimumBookingDurationMinutes(60)
                .maximumBookingDurationMinutes(180)
                .bookingIntervalMinutes(60)
                .build();
        SubField subField = SubField.builder()
                .id(subFieldId)
                .field(field)
                .name("Pitch A")
                .subFieldType(SubFieldType.FOOTBALL_5V5)
                .bookingRule(bookingRule)
                .timePriceRules(new ArrayList<>())
                .build();
        SubFieldRepository subFieldRepository = mock(SubFieldRepository.class);
        when(subFieldRepository.findById(subFieldId)).thenReturn(java.util.Optional.of(subField));
        when(subFieldRepository.save(subField)).thenReturn(subField);
        SubFieldServiceImpl service = new SubFieldServiceImpl(
                subFieldRepository,
                mock(FieldRepository.class),
                mock(FieldOperatingHoursRepository.class),
                mock(SubFieldMapper.class),
                mock(FieldEventPublisher.class));
        SubFieldRequest request = SubFieldRequest.builder()
                .bookingRule(BookingRuleDto.builder().maximumBookingDurationMinutes(240).build())
                .build();

        service.update(subFieldId, request);

        assertEquals("Pitch A", subField.getName());
        assertEquals(60, bookingRule.getMinimumBookingDurationMinutes());
        assertEquals(240, bookingRule.getMaximumBookingDurationMinutes());
        assertEquals(60, bookingRule.getBookingIntervalMinutes());
    }

    @Test
    void createRejectsTimePriceRulesThatDoNotCoverOpenOperatingHours() {
        UUID fieldId = UUID.randomUUID();
        FieldType football = FieldType.builder().id(1L).name(SportType.FOOTBALL).build();
        Field field = Field.builder().id(fieldId).name("Sports Center")
                .fieldTypes(Set.of(football)).build();
        FieldRepository fieldRepository = mock(FieldRepository.class);
        FieldOperatingHoursRepository operatingHoursRepository = mock(FieldOperatingHoursRepository.class);
        when(fieldRepository.findById(fieldId)).thenReturn(java.util.Optional.of(field));
        when(operatingHoursRepository.findByFieldId(fieldId)).thenReturn(List.of(FieldOperatingHours.builder()
                .fieldId(fieldId)
                .dayOfWeek(DayOfWeek.MONDAY)
                .openTime(LocalTime.of(6, 0))
                .closeTime(LocalTime.of(23, 0))
                .closed(false)
                .build()));

        SubFieldServiceImpl service = new SubFieldServiceImpl(
                mock(SubFieldRepository.class),
                fieldRepository,
                operatingHoursRepository,
                mock(SubFieldMapper.class),
                mock(FieldEventPublisher.class));
        SubFieldRequest request = SubFieldRequest.builder()
                .subFieldType(SubFieldType.FOOTBALL_5V5)
                .name("Pitch A")
                .timePriceRules(List.of(TimePriceRuleDto.builder()
                        .startTime(LocalTime.of(6, 0))
                        .endTime(LocalTime.of(22, 0))
                        .hourlyPrice(java.math.BigDecimal.TEN)
                        .build()))
                .build();

        assertThrows(BadRequestException.class, () -> service.create(fieldId, request));
    }
}
