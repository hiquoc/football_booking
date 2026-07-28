package com.project.field.service.impl;

import com.project.common.dto.ApiResponse;
import com.project.common.dto.PageResponse;
import com.project.common.enums.UserType;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.ForbiddenException;
import com.project.common.exception.NotFoundException;
import com.project.field.client.UserServiceClient;
import com.project.field.dto.FieldDto;
import com.project.field.dto.FieldEmployeeDto;
import com.project.field.dto.UserDto;
import com.project.field.entity.Field;
import com.project.field.entity.FieldEmployeeAssignment;
import com.project.field.exceptions.FieldNotFoundException;
import com.project.field.mapper.FieldMapper;
import com.project.field.repository.FieldEmployeeAssignmentRepository;
import com.project.field.repository.FieldRepository;
import com.project.field.service.FieldEmployeeAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FieldEmployeeAssignmentServiceImpl implements FieldEmployeeAssignmentService {
    private final FieldRepository fieldRepository;
    private final FieldEmployeeAssignmentRepository assignmentRepository;
    private final UserServiceClient userServiceClient;
    private final FieldMapper fieldMapper;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"field-detail", "field-search"}, allEntries = true)
    public FieldEmployeeDto assign(UUID ownerId, UUID fieldId, UUID employeeId) {
        Field field = requireOwnedField(ownerId, fieldId);
        UserDto employee = requireEmployee(employeeId);
        if (assignmentRepository.existsByFieldIdAndEmployeeId(fieldId, employeeId)) {
            throw new BadRequestException("Employee is already assigned to this field");
        }
        FieldEmployeeAssignment saved = assignmentRepository.save(FieldEmployeeAssignment.builder()
                .field(field)
                .employeeId(employeeId)
                .build());
        return toDto(saved, employee);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"field-detail", "field-search"}, allEntries = true)
    public void remove(UUID ownerId, UUID fieldId, UUID employeeId) {
        requireOwnedField(ownerId, fieldId);
        FieldEmployeeAssignment assignment = assignmentRepository.findByFieldIdAndEmployeeId(fieldId, employeeId)
                .orElseThrow(() -> new NotFoundException("Employee assignment not found"));
        assignmentRepository.delete(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldEmployeeDto> getFieldEmployees(UUID ownerId, UUID fieldId) {
        requireOwnedField(ownerId, fieldId);
        return assignmentRepository.findByFieldIdOrderByCreatedAtAsc(fieldId).stream()
                .map(assignment -> toDto(assignment, getUser(assignment.getEmployeeId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FieldDto> getAssignedFields(UUID employeeId, Pageable pageable) {
        List<Field> fields = assignmentRepository.findByEmployeeIdOrderByCreatedAtAsc(employeeId).stream()
                .map(FieldEmployeeAssignment::getField)
                .toList();
        int start = Math.min((int) pageable.getOffset(), fields.size());
        int end = Math.min(start + pageable.getPageSize(), fields.size());
        return PageResponse.from(new PageImpl<>(fields.subList(start, end), pageable, fields.size()).map(fieldMapper::toDto));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getAssignedFieldIds(UUID employeeId) {
        return assignmentRepository.findByEmployeeIdOrderByCreatedAtAsc(employeeId).stream()
                .map(assignment -> assignment.getField().getId())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canManageField(UUID userId, UUID fieldId) {
        return fieldRepository.findById(fieldId)
                .map(field -> field.getOwnerId().equals(userId) || assignmentRepository.existsByEmployeeIdAndFieldId(userId, fieldId))
                .orElse(false);
    }

    private Field requireOwnedField(UUID ownerId, UUID fieldId) {
        Field field = fieldRepository.findById(fieldId).orElseThrow(() -> new FieldNotFoundException(fieldId));
        if (!field.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("Only the field owner can manage employee assignments");
        }
        return field;
    }

    private UserDto requireEmployee(UUID employeeId) {
        UserDto employee = getUser(employeeId);
        if (employee.getUserType() != UserType.EMPLOYEE) {
            throw new BadRequestException("Only users with the EMPLOYEE role can be assigned");
        }
        return employee;
    }

    private UserDto getUser(UUID userId) {
        ApiResponse<UserDto> response = userServiceClient.getUserProfile(userId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new NotFoundException("User not found with id " + userId);
        }
        return response.getData();
    }

    private FieldEmployeeDto toDto(FieldEmployeeAssignment assignment, UserDto employee) {
        return FieldEmployeeDto.builder()
                .assignmentId(assignment.getId())
                .fieldId(assignment.getField().getId())
                .employeeId(assignment.getEmployeeId())
                .phoneNumber(employee.getPhoneNumber())
                .fullName(employee.getFullName())
                .email(employee.getEmail())
                .avatarUrl(employee.getAvatarUrl())
                .assignedAt(assignment.getCreatedAt())
                .build();
    }
}
