package com.project.field.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class FieldImageOrderRequest {

    @NotEmpty(message = "Ordered image IDs are required")
    private List<@NotNull(message = "Image ID must not be null") Long> imageIds;
}
