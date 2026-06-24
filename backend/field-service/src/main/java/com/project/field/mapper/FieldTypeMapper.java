package com.project.field.mapper;

import com.project.common.enums.SubFieldType;
import com.project.field.dto.FieldTypeDto;
import com.project.field.dto.FieldTypeRequest;
import com.project.field.entity.FieldType;
import org.springframework.stereotype.Component;

@Component
public class FieldTypeMapper {

    public FieldTypeDto toDto(FieldType entity) {
        if (entity == null) return null;
        return FieldTypeDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .allowedSubFieldTypes(SubFieldType.forFieldType(entity.getName()))
                .defaultBookingDurationMinutes(entity.getDefaultBookingDurationMinutes())
                .description(entity.getDescription())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public FieldType toEntity(FieldTypeRequest request) {
        if (request == null) return null;
        return FieldType.builder()
                .name(request.getName())
                .defaultBookingDurationMinutes(request.getDefaultBookingDurationMinutes())
                .description(request.getDescription())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
    }
}
