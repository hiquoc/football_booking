package com.project.booking.dto.request;

import com.project.booking.enums.WinningTeam;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpsertMatchResultRequest {
    @NotNull
    private WinningTeam winningTeam;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer teamAPercentage;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer teamBPercentage;
}
