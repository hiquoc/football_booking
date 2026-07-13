package com.project.field.service.impl;

import com.project.common.exception.BadRequestException;
import com.project.common.cache.CacheNames;
import com.project.field.dto.FieldTypeDto;
import com.project.field.dto.FieldTypeRequest;
import com.project.field.entity.FieldType;
import com.project.field.mapper.FieldTypeMapper;
import com.project.field.repository.FieldTypeRepository;
import com.project.field.service.FieldTypeService;
import com.project.field.exceptions.FieldTypeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FieldTypeServiceImpl implements FieldTypeService {

    private final FieldTypeRepository fieldTypeRepository;
    private final FieldTypeMapper fieldTypeMapper;

    @Override
    @CacheEvict(cacheNames = CacheNames.FIELD_TYPES, allEntries = true)
    public FieldTypeDto create(FieldTypeRequest request) {
        validate(request);
        FieldType entity = fieldTypeMapper.toEntity(request);
        return fieldTypeMapper.toDto(fieldTypeRepository.save(entity));
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.FIELD_TYPES, allEntries = true)
    public FieldTypeDto update(Long id, FieldTypeRequest request) {
        FieldType entity = fieldTypeRepository.findById(id)
                .orElseThrow(() -> new FieldTypeNotFoundException(id));
        validate(request);
        entity.setName(request.getName());
        entity.setDefaultBookingDurationMinutes(request.getDefaultBookingDurationMinutes());
        entity.setDescription(request.getDescription());
        entity.setActive(request.getActive() != null ? request.getActive() : entity.getActive());
        return fieldTypeMapper.toDto(fieldTypeRepository.save(entity));
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.FIELD_TYPES, allEntries = true)
    public void delete(Long id) {
        fieldTypeRepository.deleteById(id);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.FIELD_TYPES, key = "'lookup:field-types'", sync = true)
    public List<FieldTypeDto> getAll() {
        return fieldTypeRepository.findAll().stream()
                .map(fieldTypeMapper::toDto)
                .collect(Collectors.toList());
    }

    private void validate(FieldTypeRequest request) {
        if (request.getName() == null) {
            throw new BadRequestException("Field type name is required");
        }
        if (request.getDefaultBookingDurationMinutes() == null
                || request.getDefaultBookingDurationMinutes() <= 0) {
            throw new BadRequestException("Default booking duration must be greater than 0 minutes");
        }
    }
}
