package com.project.field.service;

import com.project.field.dto.FieldTypeDto;
import com.project.field.dto.FieldTypeRequest;

import java.util.List;

public interface FieldTypeService {
    FieldTypeDto create(FieldTypeRequest request);
    FieldTypeDto update(Long id, FieldTypeRequest request);
    void delete(Long id);
    List<FieldTypeDto> getAll();
}
