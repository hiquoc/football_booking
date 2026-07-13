package com.project.gateway.ratelimit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class RedisTokenBucketRateLimiterIntegrationTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private ReactiveStringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new ReactiveStringRedisTemplate(connectionFactory);
        redisTemplate.delete(redisTemplate.keys("gateway:rate-limit:test:*")).block();
    }

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void sharedRedisStateSurvivesLimiterRecreationAndMultipleInstances() {
        RateLimitProperties.Policy policy = new RateLimitProperties.Policy();
        policy.setReplenishRate(1);
        policy.setBurstCapacity(1);
        String key = "gateway:rate-limit:test:shared";

        RedisTokenBucketRateLimiter firstGatewayInstance = new RedisTokenBucketRateLimiter(redisTemplate);
        RedisTokenBucketRateLimiter secondGatewayInstance = new RedisTokenBucketRateLimiter(redisTemplate);
        RedisTokenBucketRateLimiter restartedGatewayInstance = new RedisTokenBucketRateLimiter(redisTemplate);

        assertThat(firstGatewayInstance.isAllowed(key, policy).block()).isTrue();
        assertThat(secondGatewayInstance.isAllowed(key, policy).block()).isFalse();
        assertThat(restartedGatewayInstance.isAllowed(key, policy).block()).isFalse();
    }
}
