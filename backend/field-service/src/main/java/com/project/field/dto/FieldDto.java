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
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phoneNumber;
    private String email;
    private Boolean active;
    private FieldStatus status;
    private BigDecimal ratingAverage;
    private Integer totalReviews;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FieldImageDto> images;
    /** The sport categories (FieldTypes) supported by this venue. */
    private List<FieldTypeDto> fieldTypes;
}
