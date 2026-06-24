package com.project.user.service;

import java.util.UUID;

public interface RedisService {
    void set(String key, Object value, long timeoutSeconds);

    Object get(String key);

    boolean hasKey(String key);

    void delete(String key);

    Long increment(String key);

    Long getExpire(String key);

    void trackRefreshToken(UUID userId, String refreshToken, long expirationMs);

    boolean isRefreshTokenValid(UUID userId, String refreshToken);

    void removeRefreshToken(UUID userId, String refreshToken);
}
