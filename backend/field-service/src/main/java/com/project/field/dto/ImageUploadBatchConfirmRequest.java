package com.project.field.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record ImageUploadBatchConfirmRequest(
        @NotEmpty @Size(max = 10) List<@Valid ImageUploadConfirmRequest> uploads) {}
