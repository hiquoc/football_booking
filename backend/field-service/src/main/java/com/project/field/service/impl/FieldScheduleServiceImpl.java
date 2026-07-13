package com.project.field.service.impl;

import com.project.common.exception.BadRequestException;
import com.project.common.exception.ForbiddenException;
import com.project.common.exception.NotFoundException;
import com.project.common.cache.CacheNames;
import com.project.field.dto.FieldClosureDto;
import com.project.field.client.BookingServiceClient;
import com.project.field.dto.FieldClosureRequest;
import com.project.field.dto.OperatingHoursDto;
import com.project.field.dto.OperatingHoursRequest;
import com.project.field.entity.Field;
import com.project.field.entity.SubFieldClosure;
import com.project.field.entity.FieldOperatingHours;
import com.project.field.entity.SubField;
import com.project.field.entity.SubFieldOperatingHours;
import com.project.field.exceptions.FieldNotFoundException;
import com.project.field.exceptions.SubFieldNotFoundException;
import com.project.field.kafka.FieldEventPublisher;
import com.project.field.repository.FieldClosureRepository;
import com.project.field.repository.FieldOperatingHoursRepository;
import com.project.field.repository.FieldRepository;
import com.project.field.repository.SubFieldOperatingHoursRepository;
import com.project.field.repository.SubFieldRepository;
import com.project.field.service.FieldScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FieldScheduleServiceImpl implements FieldScheduleService {

    private final FieldRepository fieldRepository;
    private final SubFieldRepository subFieldRepository;
    private final FieldOperatingHoursRepository fieldOperatingHoursRepository;
    private final SubFieldOperatingHoursRepository subFieldOperatingHoursRepository;
    private final FieldClosureRepository fieldClosureRepository;
    private final FieldEventPublisher fieldEventPublisher;
    private final BookingServiceClient bookingServiceClient;

    @Override
    @Transactional(readOnly = true)
    public List<OperatingHoursDto> getFieldOperatingHours(UUID fieldId) {
        requireField(fieldId);
        return fieldOperatingHoursRepository.findByFieldId(fieldId).stream()
                .sorted(Comparator.comparing(FieldOperatingHours::getDayOfWeek))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
    public List<OperatingHoursDto> replaceFieldOperatingHours(UUID fieldId, UUID currentUserId, String role,
            List<OperatingHoursRequest> requests) {
        Field field = requireField(fieldId);
        verifyCanManage(field, currentUserId, role);
        validateCompleteWeek(requests);

        Map<DayOfWeek, FieldOperatingHours> existingByDay = fieldOperatingHoursRepository.findByFieldId(fieldId)
                .stream()
                .collect(Collectors.toMap(FieldOperatingHours::getDayOfWeek, Function.identity()));

        List<FieldOperatingHours> hours = requests.stream()
                .map(request -> {
                    validateHours(request);
                    FieldOperatingHours operatingHours = existingByDay.getOrDefault(
                            request.getDayOfWeek(),
                            FieldOperatingHours.builder()
                                    .fieldId(fieldId)
                                    .dayOfWeek(request.getDayOfWeek())
                                    .build());
                    apply(operatingHours, request);
                    return operatingHours;
                })
                .toList();

        List<FieldOperatingHours> saved = fieldOperatingHoursRepository.saveAll(hours);
        fieldEventPublisher.publishFieldOperatingHoursUpdated(saved);
        return saved.stream().sorted(Comparator.comparing(FieldOperatingHours::getDayOfWeek)).map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperatingHoursDto> getSubFieldOperatingHours(UUID subFieldId) {
        requireSubField(subFieldId);
        return subFieldOperatingHoursRepository.findBySubFieldId(subFieldId).stream()
                .sorted(Comparator.comparing(SubFieldOperatingHours::getDayOfWeek))
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
    public List<OperatingHoursDto> replaceSubFieldOperatingHours(UUID subFieldId, UUID currentUserId, String role,
            List<OperatingHoursRequest> requests) {
        SubField subField = requireSubField(subFieldId);
        verifyCanManage(subField.getField(), currentUserId, role);
        validateCompleteWeek(requests);

        Map<DayOfWeek, SubFieldOperatingHours> existingByDay = subFieldOperatingHoursRepository.findBySubFieldId(subFieldId)
                .stream()
                .collect(Collectors.toMap(SubFieldOperatingHours::getDayOfWeek, Function.identity()));

        List<SubFieldOperatingHours> hours = requests.stream()
                .map(request -> {
                    validateHours(request);
                    SubFieldOperatingHours operatingHours = existingByDay.getOrDefault(
                            request.getDayOfWeek(),
                            SubFieldOperatingHours.builder()
                                    .subFieldId(subFieldId)
                                    .dayOfWeek(request.getDayOfWeek())
                                    .build());
                    apply(operatingHours, request);
                    return operatingHours;
                })
                .toList();

        List<SubFieldOperatingHours> saved = subFieldOperatingHoursRepository.saveAll(hours);
        fieldEventPublisher.publishSubFieldOperatingHoursUpdated(saved);
        return saved.stream().sorted(Comparator.comparing(SubFieldOperatingHours::getDayOfWeek)).map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldClosureDto> getClosures(UUID subFieldId) {
        requireSubField(subFieldId);
        return fieldClosureRepository.findBySubFieldIdOrderByStartDateDesc(subFieldId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
    public List<FieldClosureDto> createClosures(UUID currentUserId, String role, FieldClosureRequest request) {
        validateClosure(request);

        List<SubField> subFields = request.getSubFieldIds().stream()
                .distinct()
                .map(this::requireSubField)
                .toList();
        subFields.forEach(subField -> verifyCanManage(subField.getField(), currentUserId, role));

        Set<UUID> requestedSubFieldIds = subFields.stream()
                .map(SubField::getId)
                .collect(Collectors.toSet());
        rejectBookingConflicts(requestedSubFieldIds, request);
        Set<UUID> subFieldIdsWithExistingClosure = fieldClosureRepository
                .findOverlappingClosures(requestedSubFieldIds, request.getStartDate(), request.getEndDate())
                .stream()
                .map(SubFieldClosure::getSubFieldId)
                .collect(Collectors.toSet());

        List<SubFieldClosure> closures = subFields.stream()
                .filter(subField -> !subFieldIdsWithExistingClosure.contains(subField.getId()))
                .map(subField -> SubFieldClosure.builder()
                        .subFieldId(subField.getId())
                        .startDate(request.getStartDate())
                        .endDate(request.getEndDate())
                        .reason(request.getReason())
                        .build())
                .toList();

        List<SubFieldClosure> saved = fieldClosureRepository.saveAll(closures);
        fieldEventPublisher.publishClosureCreated(saved);
        return saved.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
    public FieldClosureDto updateClosure(UUID closureId, UUID currentUserId, String role, FieldClosureRequest request) {
        SubFieldClosure closure = fieldClosureRepository.findById(closureId)
                .orElseThrow(() -> new NotFoundException("Field closure not found with id: " + closureId));
        SubField subField = requireSubField(closure.getSubFieldId());
        verifyCanManage(subField.getField(), currentUserId, role);
        validateClosure(request);
        rejectBookingConflicts(Set.of(closure.getSubFieldId()), request);

        closure.setStartDate(request.getStartDate());
        closure.setEndDate(request.getEndDate());
        closure.setReason(request.getReason());
        SubFieldClosure saved = fieldClosureRepository.save(closure);
        fieldEventPublisher.publishClosureUpdated(saved);
        return toDto(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {CacheNames.FIELD_DETAIL, CacheNames.FIELD_SEARCH}, allEntries = true)
    public void deleteClosure(UUID closureId, UUID currentUserId, String role) {
        SubFieldClosure closure = fieldClosureRepository.findById(closureId)
                .orElseThrow(() -> new NotFoundException("Field closure not found with id: " + closureId));
        SubField subField = requireSubField(closure.getSubFieldId());
        verifyCanManage(subField.getField(), currentUserId, role);
        fieldEventPublisher.publishClosureDeleted(closure);
        fieldClosureRepository.delete(closure);
    }

    private Field requireField(UUID fieldId) {
        return fieldRepository.findById(fieldId)
                .orElseThrow(() -> new FieldNotFoundException(fieldId));
    }

    private SubField requireSubField(UUID subFieldId) {
        return subFieldRepository.findWithFieldById(subFieldId)
                .orElseThrow(() -> new SubFieldNotFoundException(subFieldId));
    }

    private void verifyCanManage(Field field, UUID currentUserId, String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return;
        }
        if (!field.getOwnerId().equals(currentUserId)) {
            throw new ForbiddenException("You don't have permission to do this");
        }
    }

    private void validateCompleteWeek(List<OperatingHoursRequest> requests) {
        if (requests == null || requests.size() != DayOfWeek.values().length) {
            throw new BadRequestException("Operating hours must include exactly one record for each day of week");
        }
        if (requests.stream().anyMatch(request -> request.getDayOfWeek() == null)) {
            throw new BadRequestException("Day of week is required");
        }

        EnumSet<DayOfWeek> days = requests.stream()
                .map(OperatingHoursRequest::getDayOfWeek)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
        if (days.size() != DayOfWeek.values().length) {
            throw new BadRequestException("Operating hours must include every day of week exactly once");
        }
    }

    private void validateHours(OperatingHoursRequest request) {
        if (request.getDayOfWeek() == null) {
            throw new BadRequestException("Day of week is required");
        }
        if (Boolean.TRUE.equals(request.getClosed())) {
            if (request.getOpenTime() != null || request.getCloseTime() != null) {
                throw new BadRequestException("Closed days must not include open time or close time");
            }
            return;
        }
        LocalTime openTime = request.getOpenTime();
        LocalTime closeTime = request.getCloseTime();
        if (openTime == null || closeTime == null) {
            throw new BadRequestException("Open time and close time are required for open days");
        }
        if (!closeTime.isAfter(openTime)) {
            throw new BadRequestException("Close time must be after open time");
        }
    }

    private void validateClosure(FieldClosureRequest request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BadRequestException("Closure start date and end date are required");
        }
        if (request.getSubFieldIds() == null || request.getSubFieldIds().isEmpty()) {
            throw new BadRequestException("At least one sub-field ID is required");
        }
        if (request.getSubFieldIds().stream().anyMatch(java.util.Objects::isNull)) {
            throw new BadRequestException("Sub-field IDs must not contain null values");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Closure end date cannot be before start date");
        }
    }

    private void rejectBookingConflicts(Set<UUID> subFieldIds, FieldClosureRequest request) {
        Boolean conflicts = bookingServiceClient.hasBookingConflicts(
                subFieldIds, request.getStartDate(), request.getEndDate()).getData();
        if (Boolean.TRUE.equals(conflicts)) {
            throw new BadRequestException("Cannot close a sub-field that has pending or confirmed bookings in this date range");
        }
    }

    private void apply(FieldOperatingHours hours, OperatingHoursRequest request) {
        hours.setClosed(Boolean.TRUE.equals(request.getClosed()));
        hours.setOpenTime(Boolean.TRUE.equals(request.getClosed()) ? null : request.getOpenTime());
        hours.setCloseTime(Boolean.TRUE.equals(request.getClosed()) ? null : request.getCloseTime());
    }

    private void apply(SubFieldOperatingHours hours, OperatingHoursRequest request) {
        hours.setClosed(Boolean.TRUE.equals(request.getClosed()));
        hours.setOpenTime(Boolean.TRUE.equals(request.getClosed()) ? null : request.getOpenTime());
        hours.setCloseTime(Boolean.TRUE.equals(request.getClosed()) ? null : request.getCloseTime());
    }

    private OperatingHoursDto toDto(FieldOperatingHours hours) {
        return OperatingHoursDto.builder()
                .id(hours.getId())
                .fieldId(hours.getFieldId())
                .dayOfWeek(hours.getDayOfWeek())
                .openTime(hours.getOpenTime())
                .closeTime(hours.getCloseTime())
                .closed(hours.getClosed())
                .build();
    }

    private OperatingHoursDto toDto(SubFieldOperatingHours hours) {
        return OperatingHoursDto.builder()
                .id(hours.getId())
                .subFieldId(hours.getSubFieldId())
                .dayOfWeek(hours.getDayOfWeek())
                .openTime(hours.getOpenTime())
                .closeTime(hours.getCloseTime())
                .closed(hours.getClosed())
                .build();
    }

    private FieldClosureDto toDto(SubFieldClosure closure) {
        return FieldClosureDto.builder()
                .id(closure.getId())
                .subFieldId(closure.getSubFieldId())
                .startDate(closure.getStartDate())
                .endDate(closure.getEndDate())
                .reason(closure.getReason())
                .build();
    }
}
