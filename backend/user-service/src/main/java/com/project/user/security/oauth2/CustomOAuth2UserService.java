package com.project.user.security.oauth2;

import com.project.common.enums.UserType;
import com.project.user.entity.User;
import com.project.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        
        return processOAuth2User(provider, oAuth2User);
    }

    private OAuth2User processOAuth2User(String provider, OAuth2User oAuth2User) {
        String providerId = oAuth2User.getName();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String avatarUrl = oAuth2User.getAttribute("picture"); // Assuming Google, for FB might be different

        Optional<User> userOpt = userRepository.findBySocialProviderAndSocialProviderId(provider, providerId);
        if (userOpt.isEmpty() && email != null) {
            userOpt = userRepository.findByEmail(email);
        }

        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
            if (user.getSocialProvider() == null) {
                user.setSocialProvider(provider);
                user.setSocialProviderId(providerId);
                user = userRepository.save(user);
            }
        } else {
            user = User.builder()
                    .email(email)
                    .fullName(name)
                    .avatarUrl(avatarUrl)
                    .socialProvider(provider)
                    .socialProviderId(providerId)
                    .userType(UserType.CLIENT)
                    .status("ACTIVE")
                    .build();
            user = userRepository.save(user);
            log.info("Registered new social user: {}", email);
        }

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }
}
