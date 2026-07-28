package com.project.field.service;

import com.project.common.dto.PageResponse;
import com.project.field.dto.FieldDto;
import com.project.field.dto.FieldEmployeeDto;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface FieldEmployeeAssignmentService {
    FieldEmployeeDto assign(UUID ownerId, UUID fieldId, UUID employeeId);
    void remove(UUID ownerId, UUID fieldId, UUID employeeId);
    List<FieldEmployeeDto> getFieldEmployees(UUID ownerId, UUID fieldId);
    PageResponse<FieldDto> getAssignedFields(UUID employeeId, Pageable pageable);
    List<UUID> getAssignedFieldIds(UUID employeeId);
    boolean canManageField(UUID userId, UUID fieldId);
}
