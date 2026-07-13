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
    private final SubFieldMapper subFieldMapper;

    public FieldMapper(FieldTypeMapper fieldTypeMapper, SubFieldMapper subFieldMapper) {
        this.fieldTypeMapper = fieldTypeMapper;
        this.subFieldMapper = subFieldMapper;
    }

    public FieldDto toDto(Field entity) {
        return toDto(entity, null);
    }

    public FieldDto toDto(Field entity, Boolean isFavorite) {
        if (entity == null) return null;
        return FieldDto.builder()
                .id(entity.getId())
                .ownerId(entity.getOwnerId())
                .name(entity.getName())
                .description(entity.getDescription())
                .address(entity.getAddress())
                .ward(entity.getWard())
                .wardCode(entity.getWardCode())
                .province(entity.getProvince())
                .provinceCode(entity.getProvinceCode())
                .legacyWard(entity.getLegacyWard())
                .legacyWardCode(entity.getLegacyWardCode())
                .legacyDistrict(entity.getLegacyDistrict())
                .legacyProvince(entity.getLegacyProvince())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .phoneNumber(entity.getPhoneNumber())
                .email(entity.getEmail())
                .active(entity.getActive())
                .status(entity.getStatus())
                .ratingAverage(entity.getRatingAverage())
                .totalReviews(entity.getTotalReviews())
                .isFavorite(isFavorite)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .images(entity.getImages() != null
                        ? entity.getImages().stream()
                                .filter(image -> image.getImageUrl() != null)
                                .sorted(Comparator.comparing(FieldImage::getDisplayOrder)
                                        .thenComparing(FieldImage::getId))
                                .map(this::toImageDto)
                                .collect(Collectors.toList())
                        : null)
                .fieldTypes(entity.getFieldTypes() != null
                        ? entity.getFieldTypes().stream().map(fieldTypeMapper::toDto).collect(Collectors.toList())
                        : null)
                .subFields(entity.getSubFields() != null
                        ? entity.getSubFields().stream().map(subFieldMapper::toDto).collect(Collectors.toList())
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
                .ward(request.getWard())
                .wardCode(request.getWardCode())
                .province(request.getProvince())
                .provinceCode(request.getProvinceCode())
                .legacyWard(request.getLegacyWard())
                .legacyWardCode(request.getLegacyWardCode())
                .legacyDistrict(request.getLegacyDistrict())
                .legacyProvince(request.getLegacyProvince())
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
        if (request.getWard() != null) entity.setWard(request.getWard());
        if (request.getWardCode() != null) entity.setWardCode(request.getWardCode());
        if (request.getProvince() != null) entity.setProvince(request.getProvince());
        if (request.getProvinceCode() != null) entity.setProvinceCode(request.getProvinceCode());
        if (request.getLegacyWard() != null) entity.setLegacyWard(request.getLegacyWard());
        if (request.getLegacyWardCode() != null) entity.setLegacyWardCode(request.getLegacyWardCode());
        if (request.getLegacyDistrict() != null) entity.setLegacyDistrict(request.getLegacyDistrict());
        if (request.getLegacyProvince() != null) entity.setLegacyProvince(request.getLegacyProvince());
        if (request.getLatitude() != null) entity.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) entity.setLongitude(request.getLongitude());
        if (request.getPhoneNumber() != null) entity.setPhoneNumber(request.getPhoneNumber());
        if (request.getEmail() != null) entity.setEmail(request.getEmail());
        if (request.getActive() != null) entity.setActive(request.getActive());
    }
}
