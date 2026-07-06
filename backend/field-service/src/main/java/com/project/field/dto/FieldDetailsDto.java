package com.project.field.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class FieldDetailsDto {
    private FieldDto field;
    private List<OperatingHoursDto> operatingHours;
    private List<SubFieldDto> subFields;
    private List<ReviewDto> reviews;
}
