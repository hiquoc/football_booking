package com.project.field.dto;

import com.project.field.enums.FieldStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FieldStatusRequest {
    @NotNull(message = "Field status is required")
    private FieldStatus status;
}
