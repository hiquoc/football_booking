package com.project.field.service.impl;

import com.project.common.enums.SubFieldType;
import com.project.common.cache.CacheNames;
import com.project.common.exception.BadRequestException;
import com.project.field.dto.SubFieldDto;
import com.project.field.dto.SubFieldRequest;
import com.project.field.dto.TimePriceRuleDto;
import com.project.field.dto.response.SubFieldResponse;
import com.project.field.entity.BookingRule;
import com.project.field.entity.Field;
import com.project.field.entity.FieldOperatingHours;
import com.project.field.entity.SubField;
import com.project.field.entity.TimePriceRule;
import com.project.field.exceptions.FieldNotFoundException;
import com.project.field.exceptions.SubFieldNotFoundException;
import com.project.field.kafka.FieldEventPublisher;
import com.project.field.mapper.SubFieldMapper;
import com.project.field.repository.FieldOperatingHoursRepository;
import com.project.field.repository.FieldRepository;
import com.project.field.repository.SubFieldRepository;
import com.project.field.service.SubFieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubFieldServiceImpl implements SubFieldService {

    private final SubFieldRepository subFieldRepository;
    private final FieldRepository fieldRepository;
    private final FieldOperatingHoursRepository fieldOperatingHoursRepository;
    private final SubFieldMapper subFieldMapper;
    private final FieldEventPublisher fieldEventPublisher;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
    public SubFieldDto create(UUID fieldId, SubFieldRequest request) {
        Field field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new FieldNotFoundException(fieldId));
        validateSubFieldRequest(request, field);

        SubField subField = subFieldMapper.toEntity(request);
        subField.setField(field);

        if (request.getBookingRule() != null) {
            BookingRule rule = new BookingRule();
            rule.setSubField(subField);
            rule.setMinimumBookingDurationMinutes(request.getBookingRule().getMinimumBookingDurationMinutes());
            rule.setMaximumBookingDurationMinutes(request.getBookingRule().getMaximumBookingDurationMinutes());
            rule.setBookingIntervalMinutes(request.getBookingRule().getBookingIntervalMinutes());
            subField.setBookingRule(rule);
        }

        if (request.getTimePriceRules() != null) {
            List<TimePriceRule> timePriceRules = request.getTimePriceRules().stream().map(dto -> {
                TimePriceRule rule = new TimePriceRule();
                rule.setSubField(subField);
                rule.setStartTime(dto.getStartTime());
                rule.setEndTime(dto.getEndTime());
                rule.setHourlyPrice(dto.getHourlyPrice());
                return rule;
            }).collect(Collectors.toList());
            subField.setTimePriceRules(timePriceRules);
        }

        SubField saved = subFieldRepository.save(subField);
        fieldEventPublisher.publishSubFieldCreated(saved);
        return subFieldMapper.toDto(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
    public SubFieldDto update(UUID id, SubFieldRequest request) {
        SubField subField = subFieldRepository.findById(id)
                .orElseThrow(() -> new SubFieldNotFoundException(id));

        SubFieldType targetSubFieldType = request.getSubFieldType() != null
                ? request.getSubFieldType()
                : subField.getSubFieldType();
        validateSubFieldUpdate(request, subField.getField(), targetSubFieldType, subField.getBookingRule());

        if (request.getName() != null) subField.setName(request.getName());
        if (request.getDescription() != null) subField.setDescription(request.getDescription());
        if (request.getActive() != null) subField.setActive(request.getActive());
        if (request.getSubFieldType() != null) subField.setSubFieldType(request.getSubFieldType());
        if (request.getIndoorOutdoor() != null) subField.setIndoorOutdoor(request.getIndoorOutdoor());
        if (request.getSurfaceType() != null) subField.setSurfaceType(request.getSurfaceType());
        if (request.getChangingRoom() != null) subField.setChangingRoom(request.getChangingRoom());
        if (request.getShower() != null) subField.setShower(request.getShower());
        if (request.getWifi() != null) subField.setWifi(request.getWifi());
        if (request.getAirConditioning() != null) subField.setAirConditioning(request.getAirConditioning());

        if (request.getBookingRule() != null) {
            BookingRule rule = subField.getBookingRule();
            if (rule == null) {
                rule = new BookingRule();
                rule.setSubField(subField);
            }
            if (request.getBookingRule().getMinimumBookingDurationMinutes() != null) {
                rule.setMinimumBookingDurationMinutes(request.getBookingRule().getMinimumBookingDurationMinutes());
            }
            if (request.getBookingRule().getMaximumBookingDurationMinutes() != null) {
                rule.setMaximumBookingDurationMinutes(request.getBookingRule().getMaximumBookingDurationMinutes());
            }
            if (request.getBookingRule().getBookingIntervalMinutes() != null) {
                rule.setBookingIntervalMinutes(request.getBookingRule().getBookingIntervalMinutes());
            }
            subField.setBookingRule(rule);
        }

        if (request.getTimePriceRules() != null) {
            if (subField.getTimePriceRules() == null) {
                subField.setTimePriceRules(new ArrayList<>());
            } else {
                subField.getTimePriceRules().clear();
            }
            List<TimePriceRule> newRules = request.getTimePriceRules().stream().map(dto -> {
                TimePriceRule rule = new TimePriceRule();
                rule.setSubField(subField);
                rule.setStartTime(dto.getStartTime());
                rule.setEndTime(dto.getEndTime());
                rule.setHourlyPrice(dto.getHourlyPrice());
                return rule;
            }).toList();
            subField.getTimePriceRules().addAll(newRules);
        }

        SubField saved = subFieldRepository.save(subField);
        fieldEventPublisher.publishSubFieldUpdated(saved);
        return subFieldMapper.toDto(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
    public void delete(UUID id) {
        SubField subField = subFieldRepository.findById(id)
                .orElseThrow(() -> new SubFieldNotFoundException(id));
        fieldEventPublisher.publishSubFieldDeleted(subField);
        subFieldRepository.delete(subField);
    }

    @Override
    public List<SubFieldDto> getByFieldId(UUID fieldId) {
        return subFieldRepository.findByFieldId(fieldId).stream()
                .map(subFieldMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SubFieldResponse getInternalSubFieldResponse(UUID subFieldId) {
        SubField subField = subFieldRepository.findWithFieldById(subFieldId)
                .orElseThrow(() -> new SubFieldNotFoundException(subFieldId));

        Field field = subField.getField();
        SubFieldType subFieldType = subField.getSubFieldType();

        Integer minMinutes = null;
        Integer maxMinutes = null;
        Integer intervalMinutes = null;
        if (subField.getBookingRule() != null) {
            minMinutes = subField.getBookingRule().getMinimumBookingDurationMinutes();
            maxMinutes = subField.getBookingRule().getMaximumBookingDurationMinutes();
            intervalMinutes = subField.getBookingRule().getBookingIntervalMinutes();
        }

        return SubFieldResponse.builder()
                .id(subField.getId())
                .fieldId(field.getId())
                .fieldName(field.getName())
                .name(subField.getName())
                .sportType(subFieldType.getFieldType())
                .subFieldType(subFieldType)
                .status(Boolean.TRUE.equals(subField.getActive()) ? "ACTIVE" : "INACTIVE")
                .ownerId(field.getOwnerId())
                .timePriceRules(subField.getTimePriceRules() != null
                        ? subField.getTimePriceRules().stream()
                                .map(subFieldMapper::toTimePriceRuleDto).collect(Collectors.toList())
                        : null)
                .minimumBookingDurationMinutes(minMinutes)
                .maximumBookingDurationMinutes(maxMinutes)
                .bookingIntervalMinutes(intervalMinutes)
                .build();
    }

    private void validateSubFieldRequest(SubFieldRequest request, Field field) {
        validateSubFieldUpdate(request, field, request.getSubFieldType(), null);
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Sub-field name is required");
        }
        if (request.getTimePriceRules() == null || request.getTimePriceRules().isEmpty()) {
            throw new BadRequestException("Time price rules are required");
        }
    }

    private void validateSubFieldUpdate(
            SubFieldRequest request,
            Field field,
            SubFieldType subFieldType,
            BookingRule existingBookingRule) {

        if (subFieldType == null) {
            throw new BadRequestException("Sub-field type is required");
        }
        if (request.getName() != null && request.getName().isBlank()) {
            throw new BadRequestException("Sub-field name must not be blank");
        }
        validateBookingRule(request, existingBookingRule);
        validateTimePriceRules(request, field);
    }

    private void validateBookingRule(SubFieldRequest request, BookingRule existingRule) {
        if (request.getBookingRule() == null) return;

        Integer requestedMin = request.getBookingRule().getMinimumBookingDurationMinutes();
        Integer requestedMax = request.getBookingRule().getMaximumBookingDurationMinutes();
        Integer requestedInterval = request.getBookingRule().getBookingIntervalMinutes();
        Integer min = requestedMin != null ? requestedMin
                : existingRule != null ? existingRule.getMinimumBookingDurationMinutes() : null;
        Integer max = requestedMax != null ? requestedMax
                : existingRule != null ? existingRule.getMaximumBookingDurationMinutes() : null;
        Integer interval = requestedInterval != null ? requestedInterval
                : existingRule != null ? existingRule.getBookingIntervalMinutes() : null;

        if (min != null && min <= 0) {
            throw new BadRequestException("Minimum booking duration must be greater than 0 minutes");
        }
        if (max != null && max <= 0) {
            throw new BadRequestException("Maximum booking duration must be greater than 0 minutes");
        }
        if (interval != null && interval <= 0) {
            throw new BadRequestException("Booking interval must be greater than 0 minutes");
        }
        if (min != null && max != null && min > max) {
            throw new BadRequestException("Minimum booking duration cannot exceed maximum booking duration");
        }
        if (min != null && interval != null && min % interval != 0) {
            throw new BadRequestException("Minimum booking duration must align with booking interval");
        }
    }

    private void validateTimePriceRules(SubFieldRequest request, Field field) {
        if (request.getTimePriceRules() == null) return;

        for (TimePriceRuleDto rule : request.getTimePriceRules()) {
            if (rule.getStartTime() == null || rule.getEndTime() == null) {
                throw new BadRequestException("Time price rule start time and end time are required");
            }
        }

        List<TimePriceRuleDto> sortedRules = request.getTimePriceRules().stream()
                .sorted((left, right) -> left.getStartTime().compareTo(right.getStartTime()))
                .collect(Collectors.toList());

        for (int i = 0; i < sortedRules.size(); i++) {
            TimePriceRuleDto rule = sortedRules.get(i);
            if (!rule.getEndTime().isAfter(rule.getStartTime())) {
                throw new BadRequestException("Time price rule end time must be after start time");
            }
            if (rule.getHourlyPrice() == null || rule.getHourlyPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Time price rule hourly price must be greater than 0");
            }
            if (i > 0 && rule.getStartTime().isBefore(sortedRules.get(i - 1).getEndTime())) {
                throw new BadRequestException("Time price rules cannot overlap");
            }
        }
        validateTimePriceRuleCoverage(field, sortedRules);
    }

    private void validateTimePriceRuleCoverage(Field field, List<TimePriceRuleDto> sortedRules) {
        List<FieldOperatingHours> operatingHours = fieldOperatingHoursRepository.findByFieldId(field.getId());
        if (operatingHours.isEmpty()) {
            throw new BadRequestException("Field operating hours must be configured before creating sub-fields");
        }

        List<FieldOperatingHours> openHours = operatingHours.stream()
                .filter(hours -> !Boolean.TRUE.equals(hours.getClosed()))
                .toList();
        if (openHours.isEmpty()) {
            throw new BadRequestException("At least one field operating day must be open");
        }

        for (FieldOperatingHours hours : openHours) {
            validateRulesCoverInterval(sortedRules, hours.getOpenTime(), hours.getCloseTime());
        }
    }

    private void validateRulesCoverInterval(List<TimePriceRuleDto> sortedRules, LocalTime openTime, LocalTime closeTime) {
        if (openTime == null || closeTime == null) {
            throw new BadRequestException("Open field operating days must include open time and close time");
        }

        LocalTime cursor = openTime;
        for (TimePriceRuleDto rule : sortedRules) {
            if (!rule.getEndTime().isAfter(cursor) || rule.getStartTime().isAfter(cursor)) {
                continue;
            }
            cursor = rule.getEndTime();
            if (!cursor.isBefore(closeTime)) {
                return;
            }
        }

        throw new BadRequestException("Time price rules must cover all field operating hours");
    }

}
