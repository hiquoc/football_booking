package com.project.user.dto;

import com.project.common.enums.UserType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeRoleRequest {

    @NotNull(message = "Role is required")
    private UserType userType;
}
