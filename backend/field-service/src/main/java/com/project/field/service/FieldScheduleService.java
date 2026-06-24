package com.project.field.service;

import com.project.field.dto.FieldClosureDto;
import com.project.field.dto.FieldClosureRequest;
import com.project.field.dto.OperatingHoursDto;
import com.project.field.dto.OperatingHoursRequest;

import java.util.List;
import java.util.UUID;

public interface FieldScheduleService {
    List<OperatingHoursDto> getFieldOperatingHours(UUID fieldId);

    List<OperatingHoursDto> replaceFieldOperatingHours(UUID fieldId, UUID currentUserId, String role,
            List<OperatingHoursRequest> requests);

    List<OperatingHoursDto> getSubFieldOperatingHours(UUID subFieldId);

    List<OperatingHoursDto> replaceSubFieldOperatingHours(UUID subFieldId, UUID currentUserId, String role,
            List<OperatingHoursRequest> requests);

    List<FieldClosureDto> getClosures(UUID subFieldId);

    List<FieldClosureDto> createClosures(UUID currentUserId, String role, FieldClosureRequest request);

    FieldClosureDto updateClosure(UUID closureId, UUID currentUserId, String role, FieldClosureRequest request);

    void deleteClosure(UUID closureId, UUID currentUserId, String role);
}
