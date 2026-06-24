package com.project.field.service;

import com.project.field.dto.SubFieldDto;
import com.project.field.dto.SubFieldRequest;
import com.project.field.dto.response.SubFieldResponse;

import java.util.List;
import java.util.UUID;

public interface SubFieldService {
    SubFieldDto create(UUID fieldId, SubFieldRequest request);
    SubFieldDto update(UUID id, SubFieldRequest request);
    void delete(UUID id);
    List<SubFieldDto> getByFieldId(UUID fieldId);
    SubFieldResponse getInternalSubFieldResponse(UUID subFieldId);
}
