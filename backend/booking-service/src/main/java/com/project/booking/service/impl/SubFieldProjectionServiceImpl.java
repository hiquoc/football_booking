package com.project.booking.service.impl;

import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.dto.response.TimePriceRuleDto;
import com.project.booking.entity.SubFieldProjection;
import com.project.booking.entity.FieldOperatingHoursProjection;
import com.project.booking.entity.SubFieldOperatingHoursProjection;
import com.project.booking.repository.BookingSubFieldProjectionRepository;
import com.project.booking.repository.BookingTimePriceRuleProjectionRepository;
import com.project.booking.repository.FieldOperatingHoursProjectionRepository;
import com.project.booking.repository.SubFieldOperatingHoursProjectionRepository;
import com.project.booking.service.ResolvedOperatingHours;
import com.project.booking.service.SubFieldProjectionService;
import com.project.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubFieldProjectionServiceImpl implements SubFieldProjectionService {

    private final BookingSubFieldProjectionRepository subFieldRepository;
    private final BookingTimePriceRuleProjectionRepository timePriceRuleRepository;
    private final FieldOperatingHoursProjectionRepository fieldOperatingHoursRepository;
    private final SubFieldOperatingHoursProjectionRepository subFieldOperatingHoursRepository;

    @Override
    @Transactional(readOnly = true)
    public SubFieldResponse getRequiredSubField(UUID subFieldId) {
        SubFieldProjection projection = subFieldRepository.findById(subFieldId)
                .orElseThrow(() -> new NotFoundException("SubField not found with id: " + subFieldId));

        return SubFieldResponse.builder()
                .id(projection.getId())
                .fieldId(projection.getFieldId())
                .fieldName(projection.getFieldName())
                .name(projection.getName())
                .sportType(projection.getSubFieldType() != null ? projection.getSubFieldType().getFieldType() : null)
                .subFieldType(projection.getSubFieldType())
                .status(Boolean.TRUE.equals(projection.getActive()) ? "ACTIVE" : "INACTIVE")
                .active(projection.getActive())
                .ownerId(projection.getOwnerId())
                .minimumBookingDurationMinutes(projection.getMinimumBookingDurationMinutes())
                .maximumBookingDurationMinutes(projection.getMaximumBookingDurationMinutes())
                .bookingIntervalMinutes(projection.getBookingIntervalMinutes())
                .hasRecurring(projection.getHasRecurring())
                .timePriceRules(timePriceRuleRepository.findBySubFieldIdOrderByStartTimeAsc(projection.getId()).stream()
                        .map(rule -> TimePriceRuleDto.builder()
                                .startTime(rule.getStartTime())
                                .endTime(rule.getEndTime())
                                .hourlyPrice(rule.getHourlyPrice())
                                .build())
                        .toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ResolvedOperatingHours resolveOperatingHours(UUID subFieldId, DayOfWeek dayOfWeek) {
        SubFieldProjection subField = subFieldRepository.findById(subFieldId)
                .orElseThrow(() -> new NotFoundException("SubField not found with id: " + subFieldId));

        return subFieldOperatingHoursRepository.findBySubFieldIdAndDayOfWeek(subFieldId, dayOfWeek)
                .map(this::toResolvedHours)
                .orElseGet(() -> fieldOperatingHoursRepository.findByFieldIdAndDayOfWeek(subField.getFieldId(), dayOfWeek)
                        .map(this::toResolvedHours)
                        .orElseThrow(() -> new NotFoundException("Operating hours not configured for subField: "
                                + subFieldId + " on " + dayOfWeek)));
    }

    private ResolvedOperatingHours toResolvedHours(SubFieldOperatingHoursProjection projection) {
        return new ResolvedOperatingHours(
                projection.getOpenTime(),
                projection.getCloseTime(),
                Boolean.TRUE.equals(projection.getClosed()));
    }

    private ResolvedOperatingHours toResolvedHours(FieldOperatingHoursProjection projection) {
        return new ResolvedOperatingHours(
                projection.getOpenTime(),
                projection.getCloseTime(),
                Boolean.TRUE.equals(projection.getClosed()));
    }
}
