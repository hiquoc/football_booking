package com.project.user.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.common.dto.ApiResponse;
import com.project.user.dto.LogoutRequest;
import com.project.user.dto.SendOtpRequest;
import com.project.user.dto.TokenResponse;
import com.project.user.dto.VerifyOtpRequest;
import com.project.user.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "OTP-based authentication")
public class AuthController {

    private final AuthService authService;

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Operation(summary = "Send OTP", description = "Sends a one-time password to the provided phone number", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = SendOtpRequest.class), examples = @ExampleObject(name = "example", value = """
            {
              "phoneNumber": "0862470050"
            }
            """))))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP sent successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "message": "OTP sent successfully",
                      "data": null
                    }
                    """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid phone number format", content = @Content(examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "message": "Invalid phone number format",
                      "data": null
                    }
                    """)))
    })
    @PostMapping("/otp/send")
    public ApiResponse<Void> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtp(request);
        return ApiResponse.success("OTP sent successfully", null);
    }

    @Operation(summary = "Verify OTP", description = "Verifies the OTP, returns the access token, and stores the refresh token in an HttpOnly cookie. If the user does not exist, they are automatically registered as a CLIENT.", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = VerifyOtpRequest.class), examples = @ExampleObject(name = "example", value = """
            {
              "phoneNumber": "0862470050",
              "code": "111111"
            }
            """))))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP verified – JWT token returned", content = @Content(schema = @Schema(implementation = TokenResponse.class), examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "message": "OTP verified successfully",
                      "data": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                    }
                    """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired OTP", content = @Content(examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "message": "Invalid or expired OTP",
                      "data": null
                    }
                    """)))
    })
    @PostMapping("/otp/verify")
    public ApiResponse<String> verifyOtp(@Valid @RequestBody VerifyOtpRequest request, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.verifyOtp(request);
        setRefreshTokenCookie(response, tokenResponse.getRefreshToken());
        return ApiResponse.success("OTP verified successfully", tokenResponse.getAccessToken());
    }

    @Operation(summary = "Refresh Token", description = "Rotates the HttpOnly refreshToken cookie and returns a new access token. The request body is not used.")
    @PostMapping("/refresh")
    public ApiResponse<String> refreshToken(
            @Parameter(name = "refreshToken", in = ParameterIn.COOKIE, required = true,
                    description = "HttpOnly refresh token cookie")
            @CookieValue(name = "refreshToken") String refreshTokenCookie,
            HttpServletResponse response) {
        TokenResponse tokenResponse = authService.refreshToken(refreshTokenCookie);
        setRefreshTokenCookie(response, tokenResponse.getRefreshToken());
        return ApiResponse.success("Token refreshed successfully", tokenResponse.getAccessToken());
    }

    @Operation(summary = "Logout", description = "Invalidate a refresh token")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) LogoutRequest request,
            @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie,
            HttpServletResponse response) {
        authService.logout(request, refreshTokenCookie);
        ResponseCookie cookie = ResponseCookie
                .from("refreshToken", null)
                .maxAge(0).path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ApiResponse.success("Logged out successfully", null);
    }

}
