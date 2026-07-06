package com.project.gateway.filter;

import com.project.common.constants.GlobalConstants;
import com.project.common.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
public class JwtHeaderRelayFilter implements WebFilter {

    private final SecretKey key;

    public JwtHeaderRelayFilter(@Value("${jwt.secret}") String jwtSecret) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(jwtSecret);
        } catch (IllegalArgumentException ignored) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = token(exchange);
        if (!StringUtils.hasText(token)) return chain.filter(exchange);

        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            UUID userId = UUID.fromString(String.valueOf(claims.get("userId")));
            String role = claims.get("role", String.class);
            String email = claims.getSubject();
            if (!StringUtils.hasText(role)) return chain.filter(exchange);

            UserPrincipal principal = new UserPrincipal(userId, email, role);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header(GlobalConstants.HEADER_USER_ID, userId.toString())
                    .header(GlobalConstants.HEADER_USER_ROLE, role)
                    .header(GlobalConstants.HEADER_USER_EMAIL, email == null ? "" : email)
                    .build();
            return chain.filter(exchange.mutate().request(request).build())
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
        } catch (Exception ignored) {
            return chain.filter(exchange);
        }
    }

    private String token(ServerWebExchange exchange) {
        String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        if (exchange.getRequest().getPath().value().startsWith("/ws")) {
            return exchange.getRequest().getQueryParams().getFirst("ticket");
        }
        return null;
    }
}
