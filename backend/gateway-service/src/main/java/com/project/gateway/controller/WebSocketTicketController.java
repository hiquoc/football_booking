package com.project.gateway.controller;

import com.project.common.dto.ApiResponse;
import com.project.common.security.UserPrincipal;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/ws-ticket")
public class WebSocketTicketController {

    private final SecretKey key;

    public WebSocketTicketController(@Value("${jwt.secret}") String jwtSecret) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(jwtSecret);
        } catch (IllegalArgumentException ignored) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> issue(@AuthenticationPrincipal UserPrincipal user) {
        Instant expiresAt = Instant.now().plusSeconds(60);
        String ticket = Jwts.builder()
                .subject(user.email() == null ? user.id().toString() : user.email())
                .claim("userId", user.id().toString())
                .claim("role", user.role())
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return ApiResponse.success(Map.of("ticket", ticket, "expiresAt", expiresAt.toString()));
    }
}
