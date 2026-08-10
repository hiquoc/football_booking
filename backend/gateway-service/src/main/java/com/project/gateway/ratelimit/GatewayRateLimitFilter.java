package com.project.gateway.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.dto.ApiResponse;
import com.project.common.enums.ApiStatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GatewayRateLimitFilter implements GlobalFilter, Ordered {

    private static final long VIOLATION_LOG_SUPPRESSION_SECONDS = 60;

    private final RateLimitProperties properties;
    private final RateLimitEndpointMatcher endpointMatcher;
    private final RateLimitKeyGenerator keyGenerator;
    private final RedisTokenBucketRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final Map<String, Instant> violationLogTimes = new ConcurrentHashMap<>();

    public GatewayRateLimitFilter(
            RateLimitProperties properties,
            RateLimitEndpointMatcher endpointMatcher,
            RateLimitKeyGenerator keyGenerator,
            RedisTokenBucketRateLimiter rateLimiter,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.endpointMatcher = endpointMatcher;
        this.keyGenerator = keyGenerator;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!properties.isEnabled()) {
            return chain.filter(exchange);
        }
        return endpointMatcher.match(exchange)
                .map(endpoint -> applyRateLimit(exchange, chain, endpoint))
                .orElseGet(() -> chain.filter(exchange));
    }

    private Mono<Void> applyRateLimit(ServerWebExchange exchange, GatewayFilterChain chain, RateLimitProperties.Endpoint endpoint) {
        RateLimitProperties.Policy policy = properties.getPolicies().get(endpoint.getPolicy());
        if (policy == null) {
            return chain.filter(exchange);
        }

        return cacheBodyIfNeeded(exchange, endpoint)
                .flatMap(mutatedExchange -> keyGenerator.generate(mutatedExchange, endpoint)
                        .flatMap(context -> rateLimiter.isAllowed(context.redisKey(), policy)
                                .flatMap(allowed -> allowed
                                        ? chain.filter(mutatedExchange)
                                        : reject(mutatedExchange, endpoint, context))
                                .onErrorResume(exception -> handleRedisFailure(mutatedExchange, chain, endpoint, exception))));
    }

    private Mono<ServerWebExchange> cacheBodyIfNeeded(ServerWebExchange exchange, RateLimitProperties.Endpoint endpoint) {
        if (CollectionUtils.isEmpty(endpoint.getBodyFields())) {
            return Mono.just(exchange);
        }

        long contentLength = exchange.getRequest().getHeaders().getContentLength();
        if (contentLength > properties.getMaxBodyBytes()) {
            return Mono.just(exchange);
        }

        return DataBufferUtils.join(exchange.getRequest().getBody(), properties.getMaxBodyBytes())
                .map(dataBuffer -> decorateWithCachedBody(exchange, dataBuffer))
                .defaultIfEmpty(exchange);
    }

    private ServerWebExchange decorateWithCachedBody(ServerWebExchange exchange, DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        DataBufferUtils.release(dataBuffer);
        String body = new String(bytes, StandardCharsets.UTF_8);
        exchange.getAttributes().put(RateLimitKeyGenerator.CACHED_BODY_ATTRIBUTE, body);

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpRequestDecorator decoratedRequest = new ServerHttpRequestDecorator(request) {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(super.getHeaders());
                headers.setContentLength(bytes.length);
                return headers;
            }

            @Override
            public Flux<DataBuffer> getBody() {
                return Flux.defer(() -> Flux.just(exchange.getResponse().bufferFactory().wrap(bytes)));
            }
        };

        return exchange.mutate().request(decoratedRequest).build();
    }

    private Mono<Void> handleRedisFailure(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            RateLimitProperties.Endpoint endpoint,
            Throwable exception) {
        log.warn("rate_limit_redis_failure endpoint={} path={} failOpen={} message={}",
                endpoint.getId(),
                exchange.getRequest().getPath().value(),
                properties.isFailOpenOnRedisError(),
                exception.getMessage());
        if (properties.isFailOpenOnRedisError()) {
            return chain.filter(exchange);
        }
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> reject(
            ServerWebExchange exchange,
            RateLimitProperties.Endpoint endpoint,
            RateLimitKeyGenerator.KeyContext context) {
        logViolation(exchange, endpoint, context);
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<Void> response = ApiResponse.error(ApiStatusCode.RATE_LIMITED, "Too many requests. Please try again later.");

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(response);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (Exception exception) {
            return exchange.getResponse().setComplete();
        }
    }

    private void logViolation(
            ServerWebExchange exchange,
            RateLimitProperties.Endpoint endpoint,
            RateLimitKeyGenerator.KeyContext context) {
        Instant now = Instant.now();
        Instant previous = violationLogTimes.put(context.redisKey(), now);
        if (previous != null && previous.plusSeconds(VIOLATION_LOG_SUPPRESSION_SECONDS).isAfter(now)) {
            return;
        }
        log.warn("rate_limit_exceeded endpoint={} path={} clientIp={} userId={} timestamp={}",
                endpoint.getId(),
                exchange.getRequest().getPath().value(),
                context.clientIp(),
                context.userId(),
                now);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
