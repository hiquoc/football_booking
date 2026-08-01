package com.project.field.mapper;

import com.project.field.dto.BookingRuleDto;
import com.project.field.dto.SubFieldDto;
import com.project.field.dto.SubFieldRequest;
import com.project.field.entity.BookingRule;
import com.project.field.entity.SubField;
import com.project.field.entity.TimePriceRule;
import com.project.field.dto.TimePriceRuleDto;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.stream.Collectors;

@Component
public class SubFieldMapper {

    public SubFieldDto toDto(SubField entity) {
        if (entity == null) return null;
        return SubFieldDto.builder()
                .id(entity.getId())
                .fieldId(entity.getField() != null ? entity.getField().getId() : null)
                .fieldType(entity.getSubFieldType() != null ? entity.getSubFieldType().getFieldType() : null)
                .name(entity.getName())
                .description(entity.getDescription())
                .active(entity.getActive())
                .indoorOutdoor(entity.getIndoorOutdoor())
                .surfaceType(entity.getSurfaceType())
                .subFieldType(entity.getSubFieldType())
                .changingRoom(entity.getChangingRoom())
                .shower(entity.getShower())
                .wifi(entity.getWifi())
                .airConditioning(entity.getAirConditioning())
                .bookingRule(toBookingRuleDto(entity.getBookingRule()))
                .timePriceRules(entity.getTimePriceRules() != null
                        ? entity.getTimePriceRules().stream()
                          .sorted(Comparator.comparing(TimePriceRule::getStartTime))
                          .map(this::toTimePriceRuleDto).collect(Collectors.toList())
                        : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public BookingRuleDto toBookingRuleDto(BookingRule entity) {
        if (entity == null) return null;
        return BookingRuleDto.builder()
                .id(entity.getId())
                .minimumBookingDurationMinutes(entity.getMinimumBookingDurationMinutes())
                .maximumBookingDurationMinutes(entity.getMaximumBookingDurationMinutes())
                .bookingIntervalMinutes(entity.getBookingIntervalMinutes())
                .build();
    }

    public TimePriceRuleDto toTimePriceRuleDto(TimePriceRule entity) {
        if (entity == null) return null;
        return TimePriceRuleDto.builder()
                .id(entity.getId())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .hourlyPrice(entity.getHourlyPrice())
                .build();
    }

    public SubField toEntity(SubFieldRequest request) {
        if (request == null) return null;
        return SubField.builder()
                .name(request.getName())
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .subFieldType(request.getSubFieldType())
                .indoorOutdoor(request.getIndoorOutdoor())
                .surfaceType(request.getSurfaceType())
                .changingRoom(request.getChangingRoom() != null ? request.getChangingRoom() : false)
                .shower(request.getShower() != null ? request.getShower() : false)
                .wifi(request.getWifi() != null ? request.getWifi() : false)
                .airConditioning(request.getAirConditioning() != null ? request.getAirConditioning() : false)
                .build();
    }
}
