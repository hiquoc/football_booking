package com.project.booking.moderation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ReportNoShowRequest {
    @NotNull
    private UUID bookingId;
}
