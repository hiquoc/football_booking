package com.project.gateway.ratelimit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatewayRateLimitFilterTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void allowedRequestsContinueToRoute() {
        Fixture fixture = fixture(true);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/fields").build());
        when(fixture.chain.filter(exchange)).thenReturn(Mono.empty());

        fixture.filter.filter(exchange, fixture.chain).block();

        verify(fixture.chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void rejectedRequestsReturn429ErrorResponse() throws Exception {
        Fixture fixture = fixture(false);
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/fields").build());

        fixture.filter.filter(exchange, fixture.chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        JsonNode json = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(json.get("code").asText()).isEqualTo("RATE_LIMITED");
        assertThat(json.get("statusCode").asText()).isEqualTo("RATE_LIMITED");
        assertThat(json.get("status").asInt()).isEqualTo(429);
        assertThat(json.get("message").asText()).contains("Too many requests");
        verify(fixture.chain, never()).filter(exchange);
    }

    @Test
    void redisFailureFailsOpenWhenConfigured() {
        Fixture fixture = fixture(new IllegalStateException("redis unavailable"));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/fields").build());
        when(fixture.chain.filter(exchange)).thenReturn(Mono.empty());

        fixture.filter.filter(exchange, fixture.chain).block();

        verify(fixture.chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private Fixture fixture(boolean allowed) {
        return fixtureResult(Mono.just(allowed));
    }

    private Fixture fixture(Throwable error) {
        return fixtureResult(Mono.error(error));
    }

    private Fixture fixtureResult(Mono<Boolean> result) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setFailOpenOnRedisError(true);
        RateLimitProperties.Policy policy = new RateLimitProperties.Policy();
        policy.setReplenishRate(1);
        policy.setBurstCapacity(1);
        properties.setPolicies(Map.of("SEARCH_FIELDS", policy));

        RateLimitProperties.Endpoint endpoint = new RateLimitProperties.Endpoint();
        endpoint.setId("search-fields");
        endpoint.setPolicy("SEARCH_FIELDS");
        endpoint.setPaths(List.of("/api/v1/fields"));

        RateLimitEndpointMatcher matcher = mock(RateLimitEndpointMatcher.class);
        RateLimitKeyGenerator keyGenerator = mock(RateLimitKeyGenerator.class);
        RedisTokenBucketRateLimiter rateLimiter = mock(RedisTokenBucketRateLimiter.class);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        MockServerWebExchange anyExchange = MockServerWebExchange.from(MockServerHttpRequest.get("/placeholder").build());
        when(matcher.match(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.of(endpoint));
        when(keyGenerator.generate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(endpoint)))
                .thenReturn(Mono.just(new RateLimitKeyGenerator.KeyContext("redis-key", "127.0.0.1", null)));
        when(rateLimiter.isAllowed("redis-key", policy)).thenReturn(result);

        return new Fixture(
                new GatewayRateLimitFilter(properties, matcher, keyGenerator, rateLimiter, objectMapper),
                chain,
                anyExchange);
    }

    private record Fixture(GatewayRateLimitFilter filter, GatewayFilterChain chain, MockServerWebExchange ignored) {
    }
}
