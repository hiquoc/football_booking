package com.project.user.service.impl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.common.constants.GlobalConstants;
import com.project.common.enums.UserType;
import com.project.common.exception.BadRequestException;
import com.project.user.dto.LogoutRequest;
import com.project.user.dto.SendOtpRequest;
import com.project.user.dto.TokenResponse;
import com.project.user.dto.VerifyOtpRequest;
import com.project.user.entity.User;
import com.project.user.kafka.UserNotificationEventPublisher;
import com.project.user.repository.UserRepository;
import com.project.user.service.AuthService;
import com.project.user.service.RedisService;
import com.project.user.util.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RedisService redisService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserNotificationEventPublisher userNotificationEventPublisher;

    @Override
    public void sendOtp(SendOtpRequest request) {
        String phone = request.getPhoneNumber();
        String cooldownKey = GlobalConstants.REDIS_KEY_OTP_COOLDOWN_PREFIX + phone;

        // 1. Check Resend Cooldown (60 seconds)
        if (redisService.hasKey(cooldownKey)) {
            Long expire = redisService.getExpire(cooldownKey);
            throw new BadRequestException("Please wait " + expire + " seconds before requesting a new OTP");
        }

        // Random random = new Random();
        Integer otp = 111111; // random.nextInt(1000000);

        // 2. Generate 6-digit OTP
        String otpCode = String.format("%06d", otp);
        log.info("auth_otp_requested eventType=otp_requested userId=anonymous phoneSuffix={}", phoneSuffix(phone));

        // 3. Save to Redis
        String codeKey = GlobalConstants.REDIS_KEY_OTP_CODE_PREFIX + phone;
        String attemptKey = GlobalConstants.REDIS_KEY_OTP_ATTEMPTS_PREFIX + phone;

        redisService.set(codeKey, otpCode, 300); // 5 minutes TTL
        redisService.set(cooldownKey, "true", 60); // 60 seconds cooldown
        redisService.set(attemptKey, "0", 300); // Reset attempts, 5 mins TTL
        userNotificationEventPublisher.publishUserRequestOtp(phone);
    }

    @Override
    @Transactional
    public TokenResponse verifyOtp(VerifyOtpRequest request) {
        String phone = request.getPhoneNumber();
        String codeKey = GlobalConstants.REDIS_KEY_OTP_CODE_PREFIX + phone;
        String attemptKey = GlobalConstants.REDIS_KEY_OTP_ATTEMPTS_PREFIX + phone;

        // 1. Validate existence
        if (!redisService.hasKey(codeKey)) {
            throw new BadRequestException("OTP has expired or was not requested");
        }

        // 2. Validate attempt limit
        String attemptsStr = (String) redisService.get(attemptKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        if (attempts >= 5) {
            throw new BadRequestException("Too many invalid OTP attempts. Please request a new OTP.");
        }

        String actualCode = (String) redisService.get(codeKey);
        if (!actualCode.equals(request.getCode())) {
            redisService.increment(attemptKey);
            log.warn("auth_invalid_credentials eventType=invalid_otp phoneSuffix={}", phoneSuffix(phone));
            throw new BadRequestException("Invalid OTP code");
        }

        // 3. Clear Redis keys
        redisService.delete(codeKey);
        redisService.delete(attemptKey);
        redisService.delete(GlobalConstants.REDIS_KEY_OTP_COOLDOWN_PREFIX + phone);

        // 4. Retrieve or register user
        Optional<User> userOpt = userRepository.findByPhoneNumber(phone);
        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else {
            // New accounts always default to CLIENT; fullName can be updated later via profile
            String generatedName = "User " + phone.substring(phone.length() - 4);
            user = User.builder()
                    .phoneNumber(phone)
                    .fullName(generatedName)
                    .userType(UserType.CLIENT)
                    .status("ACTIVE")
                    .build();
            user = userRepository.save(user);
            log.info("auth_registration eventType=registration userId={} phoneSuffix={}", user.getId(), phoneSuffix(phone));
        }
        log.info("auth_login_success eventType=login_success userId={}", user.getId());

        // 5. Generate JWT tokens
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getUserType().name(),
                user.getCompletedBookingCount());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // 6. Track session in Redis (Max 5 sessions per user)
        redisService.trackRefreshToken(user.getId(), refreshToken, jwtTokenProvider.getRefreshExpirationInMs());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public TokenResponse refreshToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BadRequestException("Refresh token cookie is missing");
        }
        if (!jwtTokenProvider.validateToken(token) || !jwtTokenProvider.isRefreshToken(token)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        UUID userId;
        try {
            userId = jwtTokenProvider.getUserIdFromRefreshToken(token);
        } catch (RuntimeException exception) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        if (!redisService.isRefreshTokenValid(userId, token)) {
            log.warn("auth_expired_or_revoked_refresh_token eventType=expired_refresh_token userId={}", userId);
            throw new BadRequestException("Refresh token is revoked or expired");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Revoke the old refresh token (rotation)
        redisService.removeRefreshToken(userId, token);

        // Generate new access and refresh tokens
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getUserType().name(),
                user.getCompletedBookingCount());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // Track the new refresh token
        redisService.trackRefreshToken(user.getId(), newRefreshToken, jwtTokenProvider.getRefreshExpirationInMs());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Override
    public void logout(LogoutRequest request) {
        String token = request.getRefreshToken();
        
        if (jwtTokenProvider.validateToken(token)) {
            UUID userId = jwtTokenProvider.getUserIdFromJWT(token);
            redisService.removeRefreshToken(userId, token);
            log.info("auth_logout eventType=logout userId={}", userId);
        }
    }

    @Override
    public void logout(LogoutRequest request, String refreshTokenCookie) {
        String token = resolveOptionalRefreshToken(request, refreshTokenCookie);
        if (token == null) {
            return;
        }
        LogoutRequest resolvedRequest = new LogoutRequest();
        resolvedRequest.setRefreshToken(token);
        logout(resolvedRequest);
    }

    private String resolveOptionalRefreshToken(LogoutRequest request, String refreshTokenCookie) {
        return request != null && request.getRefreshToken() != null
                ? request.getRefreshToken()
                : refreshTokenCookie;
    }

    private String phoneSuffix(String phone) {
        if (phone == null || phone.length() < 4) {
            return "unknown";
        }
        return phone.substring(phone.length() - 4);
    }

}
