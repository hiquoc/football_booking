package com.project.field.dto;

import com.project.common.dto.PageResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDetailsDto {
    private FieldDto field;
    private List<OperatingHoursDto> operatingHours;
    private List<SubFieldDto> subFields;
    private PageResponse<ReviewDto> reviews;
}
