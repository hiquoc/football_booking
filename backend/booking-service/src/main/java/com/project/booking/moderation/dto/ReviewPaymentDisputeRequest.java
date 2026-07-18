package com.project.booking.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewPaymentDisputeRequest {
    @NotNull
    private Boolean approved;

    @NotBlank
    private String adminNote;
}
