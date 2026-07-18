package com.project.user.service.impl;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.Captor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.project.common.exception.BadRequestException;
import com.project.user.dto.TokenResponse;
import com.project.user.entity.User;
import com.project.user.kafka.UserNotificationEventPublisher;
import com.project.user.repository.UserRepository;
import com.project.user.service.RedisService;
import com.project.user.util.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserNotificationEventPublisher userNotificationEventPublisher;

    private AuthServiceImpl authService;

    @Captor
    private ArgumentCaptor<UUID> userIdCaptor;

    @Captor
    private ArgumentCaptor<String> oldTokenCaptor;

    @Captor
    private ArgumentCaptor<String> newTokenCaptor;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String OLD_REFRESH_TOKEN = "old-refresh-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";
    private static final String ACCESS_TOKEN = "access-token";
    private static final long REFRESH_EXPIRATION_MS = 86400000L;

    private User testUser;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                redisService,
                jwtTokenProvider,
                userNotificationEventPublisher
        );

        testUser = User.builder()
                .id(USER_ID)
                .phoneNumber("0862470050")
                .fullName("Test User")
                .userType(com.project.common.enums.UserType.CLIENT)
                .status("ACTIVE")
                .build();
    }

    @Test
    void refreshToken_shouldRotateToken_whenValid() {
        // Arrange
        when(jwtTokenProvider.validateToken(OLD_REFRESH_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(USER_ID);
        when(redisService.isRefreshTokenValid(USER_ID, OLD_REFRESH_TOKEN)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(USER_ID, null, "CLIENT", 0)).thenReturn(ACCESS_TOKEN);
        when(jwtTokenProvider.generateRefreshToken(USER_ID)).thenReturn(NEW_REFRESH_TOKEN);
        when(jwtTokenProvider.getRefreshExpirationInMs()).thenReturn(REFRESH_EXPIRATION_MS);

        // Act
        TokenResponse response = authService.refreshToken(OLD_REFRESH_TOKEN);

        // Assert
        assertNotNull(response);
        assertEquals(ACCESS_TOKEN, response.getAccessToken());
        assertEquals(NEW_REFRESH_TOKEN, response.getRefreshToken());

        // Verify old token was revoked
        verify(redisService).removeRefreshToken(USER_ID, OLD_REFRESH_TOKEN);

        // Verify new token was tracked
        verify(redisService).trackRefreshToken(USER_ID, NEW_REFRESH_TOKEN, REFRESH_EXPIRATION_MS);

        // Verify old token is no longer valid
        verify(redisService, never()).trackRefreshToken(USER_ID, OLD_REFRESH_TOKEN, REFRESH_EXPIRATION_MS);
    }

    @Test
    void refreshToken_shouldThrow_whenTokenIsInvalid() {
        // Arrange
        when(jwtTokenProvider.validateToken(OLD_REFRESH_TOKEN)).thenReturn(false);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> authService.refreshToken(OLD_REFRESH_TOKEN));

        // Verify no interaction with Redis or user repository
        verify(redisService, never()).isRefreshTokenValid(any(), any());
        verify(redisService, never()).removeRefreshToken(any(), any());
        verify(redisService, never()).trackRefreshToken(any(), any(), anyLong());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void refreshToken_shouldThrow_whenTokenIsRevoked() {
        // Arrange
        when(jwtTokenProvider.validateToken(OLD_REFRESH_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(USER_ID);
        when(redisService.isRefreshTokenValid(USER_ID, OLD_REFRESH_TOKEN)).thenReturn(false);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> authService.refreshToken(OLD_REFRESH_TOKEN));

        // Verify old token was NOT removed (it's already gone)
        verify(redisService, never()).removeRefreshToken(any(), any());
        verify(redisService, never()).trackRefreshToken(any(), any(), anyLong());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void refreshToken_shouldThrow_whenUserNotFound() {
        // Arrange
        when(jwtTokenProvider.validateToken(OLD_REFRESH_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(USER_ID);
        when(redisService.isRefreshTokenValid(USER_ID, OLD_REFRESH_TOKEN)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BadRequestException.class, () -> authService.refreshToken(OLD_REFRESH_TOKEN));

        // Verify old token was NOT removed (user not found, so we don't rotate)
        verify(redisService, never()).removeRefreshToken(any(), any());
        verify(redisService, never()).trackRefreshToken(any(), any(), anyLong());
    }

    @Test
    void refreshToken_shouldRejectReuseOfOldTokenAfterRotation() {
        // Arrange - first refresh (rotation)
        when(jwtTokenProvider.validateToken(OLD_REFRESH_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(USER_ID);
        when(redisService.isRefreshTokenValid(USER_ID, OLD_REFRESH_TOKEN)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(USER_ID, null, "CLIENT", 0)).thenReturn(ACCESS_TOKEN);
        when(jwtTokenProvider.generateRefreshToken(USER_ID)).thenReturn(NEW_REFRESH_TOKEN);
        when(jwtTokenProvider.getRefreshExpirationInMs()).thenReturn(REFRESH_EXPIRATION_MS);

        // Act - first refresh succeeds
        TokenResponse firstResponse = authService.refreshToken(OLD_REFRESH_TOKEN);
        assertNotNull(firstResponse);
        assertEquals(NEW_REFRESH_TOKEN, firstResponse.getRefreshToken());

        // Arrange - second attempt with the OLD token (should fail)
        // The old token is still valid JWT-wise, but Redis should say it's revoked
        when(redisService.isRefreshTokenValid(USER_ID, OLD_REFRESH_TOKEN)).thenReturn(false);

        // Act & Assert - second refresh with old token fails
        assertThrows(BadRequestException.class, () -> authService.refreshToken(OLD_REFRESH_TOKEN));
    }

    @Test
    void refreshToken_shouldRejectMissingCookie() {
        assertThrows(BadRequestException.class, () -> authService.refreshToken(null));
        assertThrows(BadRequestException.class, () -> authService.refreshToken(" "));
    }

    @Test
    void refreshToken_shouldRejectAccessToken() {
        when(jwtTokenProvider.validateToken(OLD_REFRESH_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken(OLD_REFRESH_TOKEN)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.refreshToken(OLD_REFRESH_TOKEN));
        verify(redisService, never()).isRefreshTokenValid(any(), any());
    }
}
