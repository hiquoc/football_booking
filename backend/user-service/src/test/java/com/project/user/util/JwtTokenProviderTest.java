package com.project.user.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        String secret = Base64.getEncoder().encodeToString(
                "test-secret-test-secret-test-secret-test-secret-test-secret".getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", secret);
        ReflectionTestUtils.setField(tokenProvider, "jwtExpirationInMs", 60_000L);
        ReflectionTestUtils.setField(tokenProvider, "refreshExpirationInMs", 60_000L);
    }

    @Test
    void extractsUserIdFromRefreshTokenSubject() {
        UUID userId = UUID.randomUUID();

        String refreshToken = tokenProvider.generateRefreshToken(userId);

        assertEquals(userId, tokenProvider.getUserIdFromRefreshToken(refreshToken));
    }

    @Test
    void rejectsAccessTokenAsRefreshToken() {
        String accessToken = tokenProvider.generateToken(
                UUID.randomUUID(), "user@example.com", "CLIENT");

        assertThrows(
                IllegalArgumentException.class,
                () -> tokenProvider.getUserIdFromRefreshToken(accessToken));
    }
}
