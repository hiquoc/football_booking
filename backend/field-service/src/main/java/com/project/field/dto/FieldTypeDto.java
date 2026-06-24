package com.project.field.dto;

import com.project.common.enums.SportType;
import com.project.common.enums.SubFieldType;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldTypeDto {
    private Long id;
    private SportType name;
    private List<SubFieldType> allowedSubFieldTypes;
    private Integer defaultBookingDurationMinutes;
    private String description;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
