package com.project.gateway.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "gateway.rate-limits")
public class RateLimitProperties {

    private boolean enabled = true;
    private boolean failOpenOnRedisError = true;
    private int maxBodyBytes = 16 * 1024;
    private Map<String, Policy> policies = new LinkedHashMap<>();
    private List<Endpoint> endpoints = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFailOpenOnRedisError() {
        return failOpenOnRedisError;
    }

    public void setFailOpenOnRedisError(boolean failOpenOnRedisError) {
        this.failOpenOnRedisError = failOpenOnRedisError;
    }

    public int getMaxBodyBytes() {
        return maxBodyBytes;
    }

    public void setMaxBodyBytes(int maxBodyBytes) {
        this.maxBodyBytes = maxBodyBytes;
    }

    public Map<String, Policy> getPolicies() {
        return policies;
    }

    public void setPolicies(Map<String, Policy> policies) {
        this.policies = policies;
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public static class Policy {
        private boolean enabled = true;
        private int replenishRate;
        private int burstCapacity;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getReplenishRate() {
            return replenishRate;
        }

        public void setReplenishRate(int replenishRate) {
            this.replenishRate = replenishRate;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }
    }

    public static class Endpoint {
        private String id;
        private boolean enabled = true;
        private String policy;
        private List<String> paths = new ArrayList<>();
        private List<HttpMethod> methods = new ArrayList<>();
        private RateLimitKeyStrategy keyStrategy = RateLimitKeyStrategy.USER_OR_IP;
        private List<String> queryFields = new ArrayList<>();
        private List<String> headerFields = new ArrayList<>();
        private List<String> bodyFields = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPolicy() {
            return policy;
        }

        public void setPolicy(String policy) {
            this.policy = policy;
        }

        public List<String> getPaths() {
            return paths;
        }

        public void setPaths(List<String> paths) {
            this.paths = paths;
        }

        public List<HttpMethod> getMethods() {
            return methods;
        }

        public void setMethods(List<HttpMethod> methods) {
            this.methods = methods;
        }

        public RateLimitKeyStrategy getKeyStrategy() {
            return keyStrategy;
        }

        public void setKeyStrategy(RateLimitKeyStrategy keyStrategy) {
            this.keyStrategy = keyStrategy;
        }

        public List<String> getQueryFields() {
            return queryFields;
        }

        public void setQueryFields(List<String> queryFields) {
            this.queryFields = queryFields;
        }

        public List<String> getHeaderFields() {
            return headerFields;
        }

        public void setHeaderFields(List<String> headerFields) {
            this.headerFields = headerFields;
        }

        public List<String> getBodyFields() {
            return bodyFields;
        }

        public void setBodyFields(List<String> bodyFields) {
            this.bodyFields = bodyFields;
        }
    }
}
