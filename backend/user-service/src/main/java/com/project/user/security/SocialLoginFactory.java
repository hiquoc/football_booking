package com.project.user.security;

import com.project.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SocialLoginFactory {

    private final List<SocialLoginProvider> providers;

    public SocialLoginProvider getProvider(String providerName) {
        for (SocialLoginProvider provider : providers) {
            if (provider.supports(providerName)) {
                return provider;
            }
        }
        throw new BadRequestException("Unsupported social login provider: " + providerName);
    }
}
