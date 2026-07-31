package com.project.booking.lock;

import com.project.booking.exception.BookingInProgressException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingLockManager {

    private static final String LOCK_KEY_PREFIX = "booking:lock:";

    private final RedisDistributedLockService lockService;

    @Value("${booking.lock.ttl-ms:10000}")
    private long lockTtlMs = 10_000;

    @Value("${booking.lock.max-attempts:3}")
    private int maxAttempts = 3;

    @Value("${booking.lock.initial-backoff-ms:50}")
    private long initialBackoffMs = 50;

    public <T> T executeWithLock(UUID subFieldId, LocalDateTime startDateTime, LocalDateTime endDateTime, Supplier<T> action) {
        List<String> keys = lockKeys(subFieldId, startDateTime, endDateTime);
        String ownerId = UUID.randomUUID().toString();
        List<String> acquiredKeys = new ArrayList<>();
        boolean acquired = false;
        try {
            acquired = acquireAll(keys, ownerId, acquiredKeys);
            if (!acquired) {
                throw new BookingInProgressException();
            }
            return action.get();
        } finally {
            releaseAll(acquiredKeys, ownerId);
        }
    }

    private boolean acquireAll(List<String> keys, String ownerId, List<String> acquiredKeys) {
        long backoffMs = initialBackoffMs;
        for (int attempt = 1; attempt <= Math.max(1, maxAttempts); attempt++) {
            acquiredKeys.clear();
            boolean complete = true;
            for (String key : keys) {
                try {
                    log.debug("redis_lock_acquire_attempt key={} attempt={} ttlMs={}", key, attempt, lockTtlMs);
                    if (lockService.tryAcquire(key, ownerId, Duration.ofMillis(lockTtlMs))) {
                        acquiredKeys.add(key);
                        log.info("redis_lock_acquired key={} attempt={}", key, attempt);
                    } else {
                        complete = false;
                        break;
                    }
                } catch (RuntimeException ex) {
                    log.error("redis_lock_failure key={} attempt={}", key, attempt, ex);
                    releaseAll(acquiredKeys, ownerId);
                    throw new BookingInProgressException();
                }
            }
            if (complete) {
                return true;
            }
            releaseAll(acquiredKeys, ownerId);
            if (attempt < maxAttempts) {
                sleep(backoffMs);
                backoffMs *= 2;
            }
        }
        log.warn("redis_lock_acquisition_timeout keys={} attempts={}", keys, maxAttempts);
        return false;
    }

    private void releaseAll(List<String> keys, String ownerId) {
        for (int i = keys.size() - 1; i >= 0; i--) {
            String key = keys.get(i);
            try {
                lockService.release(key, ownerId);
                log.info("redis_lock_released key={}", key);
            } catch (RuntimeException ex) {
                log.error("redis_lock_release_failure key={}", key, ex);
            }
        }
        keys.clear();
    }

    private void sleep(long backoffMs) {
        try {
            Thread.sleep(backoffMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BookingInProgressException();
        }
    }

    private List<String> lockKeys(UUID subFieldId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();
        List<String> keys = new ArrayList<>();
        keys.add(lockKey(subFieldId, startDate));
        if (!endDate.equals(startDate)) {
            keys.add(lockKey(subFieldId, endDate));
        }
        return keys;
    }

    private String lockKey(UUID subFieldId, LocalDate bookingDate) {
        return LOCK_KEY_PREFIX + subFieldId + ":" + bookingDate;
    }
}
