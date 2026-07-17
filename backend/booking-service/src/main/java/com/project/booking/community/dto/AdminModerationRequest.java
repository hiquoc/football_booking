package com.project.booking.community.dto;

import com.project.booking.community.enums.CommunityModerationAction;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AdminModerationRequest {
    @NotNull
    private CommunityModerationAction action;

    private UUID targetUserId;

    private UUID targetPostId;

    @NotBlank
    @Size(max = 500)
    private String reason;

    @Size(max = 1000)
    private String note;

    @Future
    private LocalDateTime expireAt;
}
