package com.project.user.service.impl;

import com.project.user.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisServiceImpl implements RedisService {

    private final StringRedisTemplate redisTemplate;

    @Override
    public void set(String key, Object value, long timeoutSeconds) {
        redisTemplate.opsForValue().set(key, String.valueOf(value), timeoutSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    @Override
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    @Override
    public void trackRefreshToken(UUID userId, String refreshToken, long expirationMs) {
        String sessionKey = "user:sessions:" + userId;
        long currentTime = System.currentTimeMillis();
        
        redisTemplate.opsForZSet().add(sessionKey, refreshToken, (double) currentTime);
        Long sessionCount = redisTemplate.opsForZSet().zCard(sessionKey);
        
        if (sessionCount != null && sessionCount > 5) {
            redisTemplate.opsForZSet().removeRange(sessionKey, 0, sessionCount - 6);
        }
        
        redisTemplate.expire(sessionKey, Duration.ofMillis(expirationMs));
    }

    @Override
    public boolean isRefreshTokenValid(UUID userId, String refreshToken) {
        String sessionKey = "user:sessions:" + userId;
        Double score = redisTemplate.opsForZSet().score(sessionKey, refreshToken);
        return score != null;
    }

    @Override
    public void removeRefreshToken(UUID userId, String refreshToken) {
        String sessionKey = "user:sessions:" + userId;
        redisTemplate.opsForZSet().remove(sessionKey, refreshToken);
    }
}
