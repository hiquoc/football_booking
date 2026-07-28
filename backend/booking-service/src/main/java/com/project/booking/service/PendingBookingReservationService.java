package com.project.booking.service;

import com.project.booking.entity.Booking;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PendingBookingReservationService {
    private static final String KEY_PREFIX = "pending-booking:";

    private final StringRedisTemplate redisTemplate;

    public boolean reserve(UUID userId, UUID bookingId, LocalDateTime expiresAt) {
        Duration ttl = Duration.between(LocalDateTime.now(), expiresAt);
        if (!ttl.isPositive()) {
            return false;
        }
        Boolean created = redisTemplate.opsForValue().setIfAbsent(key(userId), bookingId.toString(), ttl);
        return Boolean.TRUE.equals(created);
    }

    public Optional<UUID> find(UUID userId) {
        String value = redisTemplate.opsForValue().get(key(userId));
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(value));
    }

    public void release(Booking booking) {
        String key = key(booking.getClientId());
        String value = redisTemplate.opsForValue().get(key);
        if (booking.getId().toString().equals(value)) {
            redisTemplate.delete(key);
        }
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId;
    }
}
