package com.project.field.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldClosureDto {
    private UUID id;
    private UUID subFieldId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}
