package com.project.field.dto;

import com.project.common.enums.SportType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldTypeRequest {
    @NotNull(message = "Field type name is required")
    private SportType name;

    @NotNull(message = "Default booking duration is required")
    @Positive(message = "Default booking duration must be greater than 0")
    private Integer defaultBookingDurationMinutes;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Boolean active;
}
