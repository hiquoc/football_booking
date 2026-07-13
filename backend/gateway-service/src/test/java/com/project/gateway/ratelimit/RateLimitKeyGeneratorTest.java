package com.project.gateway.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitKeyGeneratorTest {

    private final RateLimitKeyGenerator keyGenerator = new RateLimitKeyGenerator(new ObjectMapper());

    @Test
    void anonymousEndpointsUseClientIp() {
        RateLimitProperties.Endpoint endpoint = endpoint(RateLimitKeyStrategy.IP);
        ServerWebExchange exchange = exchange(
                MockServerHttpRequest.post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "203.0.113.10, 10.0.0.1")
                        .build(),
                Mono.empty());

        RateLimitKeyGenerator.KeyContext context = keyGenerator.generate(exchange, endpoint).block();

        assertThat(context.clientIp()).isEqualTo("203.0.113.10");
        assertThat(context.userId()).isNull();
    }

    @Test
    void authenticatedEndpointsUseUserId() {
        UUID userId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                new UserPrincipal(userId, "user@example.com", "USER"), null, List.of());
        RateLimitProperties.Endpoint endpoint = endpoint(RateLimitKeyStrategy.USER_OR_IP);
        ServerWebExchange exchange = exchange(
                MockServerHttpRequest.get("/api/v1/users/me").build(),
                Mono.just(authentication));

        RateLimitKeyGenerator.KeyContext context = keyGenerator.generate(exchange, endpoint).block();

        assertThat(context.userId()).isEqualTo(userId.toString());
    }

    @Test
    void compositeKeysIncludeConfiguredBodyFields() {
        RateLimitProperties.Endpoint endpoint = endpoint(RateLimitKeyStrategy.COMPOSITE);
        endpoint.setBodyFields(List.of("phone"));
        ServerWebExchange first = exchange(MockServerHttpRequest.method(HttpMethod.POST, "/api/v1/auth/otp/send").build(), Mono.empty());
        ServerWebExchange second = exchange(MockServerHttpRequest.method(HttpMethod.POST, "/api/v1/auth/otp/send").build(), Mono.empty());
        first.getAttributes().put(RateLimitKeyGenerator.CACHED_BODY_ATTRIBUTE, "{\"phone\":\"84900000001\"}");
        second.getAttributes().put(RateLimitKeyGenerator.CACHED_BODY_ATTRIBUTE, "{\"phone\":\"84900000002\"}");

        String firstKey = keyGenerator.generate(first, endpoint).block().redisKey();
        String secondKey = keyGenerator.generate(second, endpoint).block().redisKey();

        assertThat(firstKey).isNotEqualTo(secondKey);
    }

    private RateLimitProperties.Endpoint endpoint(RateLimitKeyStrategy strategy) {
        RateLimitProperties.Endpoint endpoint = new RateLimitProperties.Endpoint();
        endpoint.setId("test-endpoint");
        endpoint.setPolicy("TEST_POLICY");
        endpoint.setKeyStrategy(strategy);
        return endpoint;
    }

    private ServerWebExchange exchange(MockServerHttpRequest request, Mono<? extends Principal> principal) {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        Map<String, Object> attributes = new HashMap<>();
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getPrincipal()).thenReturn((Mono) principal);
        when(exchange.getAttributes()).thenReturn(attributes);
        when(exchange.getAttribute(RateLimitKeyGenerator.CACHED_BODY_ATTRIBUTE)).thenAnswer(invocation ->
                attributes.get(RateLimitKeyGenerator.CACHED_BODY_ATTRIBUTE));
        return exchange;
    }
}
