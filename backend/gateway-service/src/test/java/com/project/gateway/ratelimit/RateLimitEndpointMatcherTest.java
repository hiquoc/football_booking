package com.project.gateway.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitEndpointMatcherTest {

    @Test
    void matchesRouteSpecificMethodAndPath() {
        RateLimitProperties.Endpoint createBooking = endpoint(
                "booking-create",
                "BOOKING_CREATE",
                List.of(HttpMethod.POST),
                List.of("/api/v1/bookings"));
        RateLimitProperties.Endpoint searchFields = endpoint(
                "search-fields",
                "SEARCH_FIELDS",
                List.of(HttpMethod.GET),
                List.of("/api/v1/fields"));
        RateLimitProperties properties = properties(createBooking, searchFields);
        RateLimitEndpointMatcher matcher = new RateLimitEndpointMatcher(properties);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/bookings").build());

        assertThat(matcher.match(exchange)).contains(createBooking);
    }

    @Test
    void ignoresDisabledEndpoints() {
        RateLimitProperties.Endpoint endpoint = endpoint(
                "admin-api",
                "ADMIN_API",
                List.of(),
                List.of("/api/v1/admin/**"));
        endpoint.setEnabled(false);
        RateLimitEndpointMatcher matcher = new RateLimitEndpointMatcher(properties(endpoint));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/admin/users").build());

        assertThat(matcher.match(exchange)).isEmpty();
    }

    private RateLimitProperties properties(RateLimitProperties.Endpoint... endpoints) {
        RateLimitProperties properties = new RateLimitProperties();
        RateLimitProperties.Policy policy = new RateLimitProperties.Policy();
        policy.setReplenishRate(1);
        policy.setBurstCapacity(1);
        properties.setPolicies(Map.of("BOOKING_CREATE", policy, "SEARCH_FIELDS", policy, "ADMIN_API", policy));
        properties.setEndpoints(List.of(endpoints));
        return properties;
    }

    private RateLimitProperties.Endpoint endpoint(String id, String policy, List<HttpMethod> methods, List<String> paths) {
        RateLimitProperties.Endpoint endpoint = new RateLimitProperties.Endpoint();
        endpoint.setId(id);
        endpoint.setPolicy(policy);
        endpoint.setMethods(methods);
        endpoint.setPaths(paths);
        return endpoint;
    }
}
