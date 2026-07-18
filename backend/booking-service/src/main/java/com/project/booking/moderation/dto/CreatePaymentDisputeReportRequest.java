package com.project.booking.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreatePaymentDisputeReportRequest {
    @NotNull
    private UUID bookingId;

    @NotBlank
    private String description;

    @NotEmpty
    private List<@NotBlank String> imageUrls;
}
