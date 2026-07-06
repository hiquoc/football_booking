package com.project.user.service;

import com.project.user.dto.LogoutRequest;
import com.project.user.dto.SendOtpRequest;
import com.project.user.dto.TokenResponse;
import com.project.user.dto.VerifyOtpRequest;

public interface AuthService {
    void sendOtp(SendOtpRequest request);

    TokenResponse verifyOtp(VerifyOtpRequest request);

    TokenResponse refreshToken(String refreshTokenCookie);

    void logout(LogoutRequest request);

    void logout(LogoutRequest request, String refreshTokenCookie);
}
