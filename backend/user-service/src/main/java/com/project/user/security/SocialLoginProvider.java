package com.project.user.security;

public interface SocialLoginProvider {
    boolean supports(String provider);
    SocialProfile verifyToken(String token);
}
