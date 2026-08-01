package com.project.user.dto;

import com.project.common.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeRoleRequest {

    @NotNull(message = "Role is required")
    @Schema(
            description = "Target user role. ADMIN cannot be assigned through role changes.",
            allowableValues = {"CLIENT", "OWNER", "EMPLOYEE"},
            example = "OWNER")
    private UserType userType;
}
