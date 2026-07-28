package com.project.field.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldCardDto {
    private UUID id;
    private String name;
    private String address;
    private String ward;
    private String province;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal ratingAverage;
    private Integer totalReviews;
    private String primaryImageUrl;
    private List<String> fieldTypes;
    private Double distanceKm;
    private Boolean isSaved;
}
