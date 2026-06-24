package com.project.user.security;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SocialProfile {
    private String id;
    private String email;
    private String name;
    private String avatarUrl;
}
