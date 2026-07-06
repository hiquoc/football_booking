package com.project.field.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record ImageUploadSlotRequest(
        @NotNull @Schema(description = "Stable key reused when retrying the same batch") UUID requestId,
        @Min(1) @Max(10) @Schema(minimum = "1", maximum = "10") int count) {}
