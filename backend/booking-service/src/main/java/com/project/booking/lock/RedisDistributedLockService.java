package com.project.booking.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisDistributedLockService {

    private static final String RELEASE_SCRIPT = """
            if redis.call("GET", KEYS[1]) == ARGV[1] then
                return redis.call("DEL", KEYS[1])
            else
                return 0
            end
            """;

    private final StringRedisTemplate redisTemplate;

    public boolean tryAcquire(String key, String ownerId, Duration ttl) {
        Boolean acquired = redisTemplate.execute((RedisCallback<Boolean>) connection -> connection.stringCommands().set(
                key.getBytes(StandardCharsets.UTF_8),
                ownerId.getBytes(StandardCharsets.UTF_8),
                Expiration.milliseconds(ttl.toMillis()),
                SetOption.SET_IF_ABSENT));
        return Boolean.TRUE.equals(acquired);
    }

    public boolean release(String key, String ownerId) {
        Long released = redisTemplate.execute((RedisCallback<Long>) connection -> connection.scriptingCommands().eval(
                RELEASE_SCRIPT.getBytes(StandardCharsets.UTF_8),
                org.springframework.data.redis.connection.ReturnType.INTEGER,
                1,
                key.getBytes(StandardCharsets.UTF_8),
                ownerId.getBytes(StandardCharsets.UTF_8)));
        return released != null && released > 0;
    }
}
