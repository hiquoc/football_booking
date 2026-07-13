package com.project.field.dto;

import com.project.field.enums.FieldStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDto {
    private UUID id;
    private UUID ownerId;
    private String name;
    private String description;
    private String address;
    private String ward;
    private String wardCode;
    private String province;
    private String provinceCode;
    private String legacyWard;
    private String legacyWardCode;
    private String legacyDistrict;
    private String legacyProvince;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phoneNumber;
    private String email;
    private Boolean active;
    private FieldStatus status;
    private BigDecimal ratingAverage;
    private Integer totalReviews;
    private Boolean isFavorite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FieldImageDto> images;
    /** The sport categories (FieldTypes) supported by this venue. */
    private List<FieldTypeDto> fieldTypes;
    private List<SubFieldDto> subFields;
}
