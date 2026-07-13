package com.project.gateway.ratelimit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.security.UserPrincipal;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class RateLimitKeyGenerator {

    static final String CACHED_BODY_ATTRIBUTE = RateLimitKeyGenerator.class.getName() + ".body";

    private final ObjectMapper objectMapper;

    public RateLimitKeyGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<KeyContext> generate(ServerWebExchange exchange, RateLimitProperties.Endpoint endpoint) {
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .map(authentication -> build(exchange, endpoint, authentication))
                .switchIfEmpty(Mono.fromSupplier(() -> build(exchange, endpoint, null)));
    }

    private KeyContext build(ServerWebExchange exchange, RateLimitProperties.Endpoint endpoint, Authentication authentication) {
        String userId = userId(authentication);
        String ip = clientIp(exchange.getRequest());
        String subject = subject(endpoint.getKeyStrategy(), userId, ip);

        StringBuilder rawKey = new StringBuilder(endpoint.getPolicy())
                .append(':')
                .append(endpoint.getId())
                .append(':')
                .append(subject);

        if (endpoint.getKeyStrategy() == RateLimitKeyStrategy.COMPOSITE) {
            appendFields(rawKey, "query", endpoint.getQueryFields(), exchange.getRequest().getQueryParams().toSingleValueMap());
            appendFields(rawKey, "header", endpoint.getHeaderFields(), exchange.getRequest().getHeaders().toSingleValueMap());
            appendFields(rawKey, "body", endpoint.getBodyFields(), bodyFields(exchange));
        }

        String digest = DigestUtils.md5DigestAsHex(rawKey.toString().getBytes(StandardCharsets.UTF_8));
        return new KeyContext("gateway:rate-limit:" + digest, ip, userId);
    }

    private String subject(RateLimitKeyStrategy strategy, String userId, String ip) {
        return switch (strategy) {
            case IP -> "ip:" + ip;
            case USER_OR_IP -> StringUtils.hasText(userId) ? "user:" + userId : "ip:" + ip;
            case COMPOSITE -> StringUtils.hasText(userId) ? "user:" + userId : "ip:" + ip;
        };
    }

    private String userId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.id().toString();
        }
        if (principal instanceof Principal javaPrincipal && StringUtils.hasText(javaPrincipal.getName())) {
            return javaPrincipal.getName();
        }
        return null;
    }

    private void appendFields(StringBuilder rawKey, String source, List<String> fields, Map<String, String> values) {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        values.forEach((key, value) -> normalized.put(key.toLowerCase(Locale.ROOT), value));
        for (String field : fields) {
            String value = normalized.get(field.toLowerCase(Locale.ROOT));
            if (StringUtils.hasText(value)) {
                rawKey.append(':').append(source).append(':').append(field).append('=').append(value.trim().toLowerCase(Locale.ROOT));
            }
        }
    }

    private Map<String, String> bodyFields(ServerWebExchange exchange) {
        String body = exchange.getAttribute(CACHED_BODY_ATTRIBUTE);
        if (!StringUtils.hasText(body)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(body, new TypeReference<>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    public String clientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddress() == null
                ? "unknown"
                : request.getRemoteAddress().getAddress().getHostAddress();
    }

    public record KeyContext(String redisKey, String clientIp, String userId) {
    }
}
