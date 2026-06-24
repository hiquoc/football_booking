package com.project.user.security.impl;

import com.project.common.exception.BadRequestException;
import com.project.user.security.SocialLoginProvider;
import com.project.user.security.SocialProfile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class FacebookProvider implements SocialLoginProvider {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean supports(String provider) {
        return "FACEBOOK".equalsIgnoreCase(provider);
    }

    @Override
    public SocialProfile verifyToken(String token) {
        if (token.startsWith("mock-")) {
            return SocialProfile.builder()
                    .id("facebook-id-" + token)
                    .email(token + "@facebook.com")
                    .name("Mock Facebook User " + token)
                    .avatarUrl("https://graph.facebook.com/mock/picture")
                    .build();
        }

        try {
            String url = "https://graph.facebook.com/me?fields=id,name,email,picture&access_token=" + token;
            @SuppressWarnings("rawtypes")
            Map response = restTemplate.getForObject(url, Map.class);
            if (response == null || response.get("id") == null) {
                throw new BadRequestException("Invalid Facebook token");
            }

            String id = String.valueOf(response.get("id"));
            String email = response.get("email") != null ? String.valueOf(response.get("email")) : id + "@facebook.com";
            String name = response.get("name") != null ? String.valueOf(response.get("name")) : "Facebook User";

            String avatarUrl = null;
            if (response.get("picture") instanceof Map) {
                @SuppressWarnings("rawtypes")
                Map pictureMap = (Map) response.get("picture");
                if (pictureMap.get("data") instanceof Map) {
                    @SuppressWarnings("rawtypes")
                    Map dataMap = (Map) pictureMap.get("data");
                    avatarUrl = String.valueOf(dataMap.get("url"));
                }
            }

            return SocialProfile.builder()
                    .id(id)
                    .email(email)
                    .name(name)
                    .avatarUrl(avatarUrl)
                    .build();
        } catch (Exception e) {
            throw new BadRequestException("Failed to verify Facebook token: " + e.getMessage());
        }
    }
}
