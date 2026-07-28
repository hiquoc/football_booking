package com.project.field.dto;

import com.project.common.enums.SubFieldType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubFieldFilterOptionDto {
    private UUID id;
    private String name;
    private String fieldName;
    private SubFieldType type;
}
