package com.project.common.security;

import java.util.UUID;

public record UserPrincipal(
        UUID id,
        String email,
        String role,
        String name
) {
    public UserPrincipal(UUID id, String email, String role) {
        this(id, email, role, null);
    }
}
