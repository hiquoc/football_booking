package com.project.gateway.ratelimit;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.List;

@Component
public class RedisTokenBucketRateLimiter {

    private static final RedisScript<Long> TOKEN_BUCKET_SCRIPT = RedisScript.of("""
            local tokens_key = KEYS[1] .. ':tokens'
            local timestamp_key = KEYS[1] .. ':timestamp'
            local rate = tonumber(ARGV[1])
            local burst = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local requested = 1

            if rate <= 0 or burst <= 0 then
              return 0
            end

            local fill_time = burst / rate
            local ttl = math.max(1, math.floor(fill_time * 2))
            local last_tokens = tonumber(redis.call('get', tokens_key))
            if last_tokens == nil then
              last_tokens = burst
            end

            local last_refreshed = tonumber(redis.call('get', timestamp_key))
            if last_refreshed == nil then
              last_refreshed = now
            end

            local delta = math.max(0, now - last_refreshed)
            local filled_tokens = math.min(burst, last_tokens + (delta * rate))
            local allowed = filled_tokens >= requested
            local new_tokens = filled_tokens
            if allowed then
              new_tokens = filled_tokens - requested
            end

            redis.call('setex', tokens_key, ttl, new_tokens)
            redis.call('setex', timestamp_key, ttl, now)

            if allowed then
              return 1
            end
            return 0
            """, Long.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final Clock clock;

    @Autowired
    public RedisTokenBucketRateLimiter(ReactiveStringRedisTemplate redisTemplate) {
        this(redisTemplate, Clock.systemUTC());
    }

    RedisTokenBucketRateLimiter(ReactiveStringRedisTemplate redisTemplate, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
    }

    public Mono<Boolean> isAllowed(String key, RateLimitProperties.Policy policy) {
        return redisTemplate.execute(TOKEN_BUCKET_SCRIPT, List.of(key), List.of(
                        String.valueOf(policy.getReplenishRate()),
                        String.valueOf(policy.getBurstCapacity()),
                        String.valueOf(clock.instant().getEpochSecond())))
                .next()
                .map(result -> result == 1L);
    }
}
