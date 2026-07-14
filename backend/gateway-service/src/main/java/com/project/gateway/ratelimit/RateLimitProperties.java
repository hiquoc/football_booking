package com.project.gateway.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@ConfigurationProperties(prefix = "gateway.rate-limits")
public class RateLimitProperties {
    private boolean failOpenOnRedisError = true;
    private int maxBodyBytes = 16 * 1024;
    private Map<String, Policy> policies = new LinkedHashMap<>();
    private List<Endpoint> endpoints = new ArrayList<>();

    @Setter
    @Getter
    public static class Policy {
        private int replenishRate;
        private int burstCapacity;

    }

    @Setter
    @Getter
    public static class Endpoint {
        private String id;
        private String policy;
        private List<String> paths = new ArrayList<>();
        private List<HttpMethod> methods = new ArrayList<>();
        private RateLimitKeyStrategy keyStrategy = RateLimitKeyStrategy.USER_OR_IP;
        private List<String> queryFields = new ArrayList<>();
        private List<String> headerFields = new ArrayList<>();
        private List<String> bodyFields = new ArrayList<>();

    }
}
