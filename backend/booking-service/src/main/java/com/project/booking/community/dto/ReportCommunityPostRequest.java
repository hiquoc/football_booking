package com.project.booking.community.dto;

import com.project.booking.community.enums.CommunityReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportCommunityPostRequest {
    @NotNull
    private CommunityReportReason reason;

    @Size(max = 1000)
    private String description;
}
