package com.project.user.controller;

import com.project.common.dto.ApiResponse;
import com.project.user.dto.SendOtpRequest;
import com.project.user.dto.TokenResponse;
import com.project.user.dto.VerifyOtpRequest;
import com.project.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CookieValue;
import com.project.user.dto.RefreshTokenRequest;
import com.project.user.dto.LogoutRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "OTP-based authentication")
public class AuthController {

    private final AuthService authService;

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Should be true in production
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(cookie);
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

    @Operation(summary = "Verify OTP", description = "Verifies the OTP and returns a JWT token pair. If the user does not exist, they are automatically registered as a CLIENT.", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = VerifyOtpRequest.class), examples = @ExampleObject(name = "example", value = """
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
                      "data": {
                        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                      }
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
    public ApiResponse<TokenResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.verifyOtp(request);
        setRefreshTokenCookie(response, tokenResponse.getRefreshToken());
        return ApiResponse.success("OTP verified successfully", tokenResponse);
    }

    @Operation(summary = "Refresh Token", description = "Get a new access token using a refresh token")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refreshToken(@Valid @RequestBody(required = false) RefreshTokenRequest request,
                                                   @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie,
                                                   HttpServletResponse response) {
        TokenResponse tokenResponse = authService.refreshToken(request, refreshTokenCookie);
        setRefreshTokenCookie(response, tokenResponse.getRefreshToken());
        return ApiResponse.success("Token refreshed successfully", tokenResponse);
    }

    @Operation(summary = "Logout", description = "Invalidate a refresh token")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) LogoutRequest request,
                                    @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie,
                                    HttpServletResponse response) {
        authService.logout(request, refreshTokenCookie);
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return ApiResponse.success("Logged out successfully", null);
    }

}
