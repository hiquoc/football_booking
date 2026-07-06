package com.project.field.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImageUploadSlotDto(
        Long imageId,
        String publicId,
        long timestamp,
        String signature,
        String apiKey,
        String cloudName,
        String uploadUrl,
        @Schema(example = "false") boolean overwrite) {}
