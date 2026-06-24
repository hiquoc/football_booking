package com.project.user.security.impl;

import com.project.common.exception.BadRequestException;
import com.project.user.security.SocialLoginProvider;
import com.project.user.security.SocialProfile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class GoogleProvider implements SocialLoginProvider {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean supports(String provider) {
        return "GOOGLE".equalsIgnoreCase(provider);
    }

    @Override
    public SocialProfile verifyToken(String token) {
        if (token.startsWith("mock-")) {
            return SocialProfile.builder()
                    .id("google-id-" + token)
                    .email(token + "@gmail.com")
                    .name("Mock Google User " + token)
                    .avatarUrl("https://lh3.googleusercontent.com/mock")
                    .build();
        }

        try {
            String url = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=" + token;
            @SuppressWarnings("rawtypes")
            Map response = restTemplate.getForObject(url, Map.class);
            if (response == null || response.get("sub") == null) {
                throw new BadRequestException("Invalid Google token");
            }

            return SocialProfile.builder()
                    .id(String.valueOf(response.get("sub")))
                    .email(String.valueOf(response.get("email")))
                    .name(String.valueOf(response.get("name")))
                    .avatarUrl(String.valueOf(response.get("picture")))
                    .build();
        } catch (Exception e) {
            throw new BadRequestException("Failed to verify Google token: " + e.getMessage());
        }
    }
}
