package com.project.field.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class FieldEmployeeAssignmentRequest {
    @NotNull
    private UUID employeeId;
}
