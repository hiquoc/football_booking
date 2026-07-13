package com.project.gateway.ratelimit;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class RateLimitEndpointMatcher {

    private final List<CompiledEndpoint> endpoints;

    public RateLimitEndpointMatcher(RateLimitProperties properties) {
        PathPatternParser parser = PathPatternParser.defaultInstance;
        this.endpoints = properties.getEndpoints().stream()
                .filter(RateLimitProperties.Endpoint::isEnabled)
                .map(endpoint -> new CompiledEndpoint(endpoint, endpoint.getPaths().stream()
                        .map(parser::parse)
                        .toList()))
                .toList();
    }

    public Optional<RateLimitProperties.Endpoint> match(ServerWebExchange exchange) {
        HttpMethod method = exchange.getRequest().getMethod();
        PathContainer path = exchange.getRequest().getPath().pathWithinApplication();

        return endpoints.stream()
                .filter(endpoint -> endpoint.matches(method, path))
                .map(CompiledEndpoint::endpoint)
                .findFirst();
    }

    private record CompiledEndpoint(RateLimitProperties.Endpoint endpoint, List<PathPattern> patterns) {
        private boolean matches(HttpMethod method, PathContainer path) {
            List<HttpMethod> methods = endpoint.getMethods() == null ? new ArrayList<>() : endpoint.getMethods();
            if (!methods.isEmpty() && !methods.contains(method)) {
                return false;
            }
            return patterns.stream().anyMatch(pattern -> pattern.matches(path));
        }
    }
}
