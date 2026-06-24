package com.project.field.mapper;

import java.util.stream.Collectors;
import java.util.UUID;
import java.util.Comparator;

import org.springframework.stereotype.Component;

import com.project.field.dto.FieldDto;
import com.project.field.dto.FieldImageDto;
import com.project.field.dto.FieldRequest;
import com.project.field.entity.Field;
import com.project.field.entity.FieldImage;

@Component
public class FieldMapper {

    private final FieldTypeMapper fieldTypeMapper;

    public FieldMapper(FieldTypeMapper fieldTypeMapper) {
        this.fieldTypeMapper = fieldTypeMapper;
    }

    public FieldDto toDto(Field entity) {
        if (entity == null) return null;
        return FieldDto.builder()
                .id(entity.getId())
                .ownerId(entity.getOwnerId())
                .name(entity.getName())
                .description(entity.getDescription())
                .address(entity.getAddress())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .phoneNumber(entity.getPhoneNumber())
                .email(entity.getEmail())
                .active(entity.getActive())
                .status(entity.getStatus())
                .ratingAverage(entity.getRatingAverage())
                .totalReviews(entity.getTotalReviews())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .images(entity.getImages() != null
                        ? entity.getImages().stream()
                                .sorted(Comparator.comparing(FieldImage::getDisplayOrder)
                                        .thenComparing(FieldImage::getId))
                                .map(this::toImageDto)
                                .collect(Collectors.toList())
                        : null)
                .fieldTypes(entity.getFieldTypes() != null
                        ? entity.getFieldTypes().stream().map(fieldTypeMapper::toDto).collect(Collectors.toList())
                        : null)
                .build();
    }

    public FieldImageDto toImageDto(FieldImage entity) {
        if (entity == null) return null;
        return FieldImageDto.builder()
                .id(entity.getId())
                .imageUrl(entity.getImageUrl())
                .isPrimary(entity.getIsPrimary())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }

    public Field toEntity(UUID ownerId, FieldRequest request) {
        if (request == null) return null;
        return Field.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
    }

    /**
     * Updates the given entity with non-null values from the request.
     * Fields with null values in the request are left unchanged (ignored).
     */
    public void updateEntity(Field entity, FieldRequest request) {
        if (request == null) return;

        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getAddress() != null) entity.setAddress(request.getAddress());
        if (request.getLatitude() != null) entity.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) entity.setLongitude(request.getLongitude());
        if (request.getPhoneNumber() != null) entity.setPhoneNumber(request.getPhoneNumber());
        if (request.getEmail() != null) entity.setEmail(request.getEmail());
        if (request.getActive() != null) entity.setActive(request.getActive());
    }
}
