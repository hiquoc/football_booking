package com.project.user.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialProfile {
    private String id;
    private String email;
    private String name;
    private String avatarUrl;
}
