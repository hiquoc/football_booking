package com.project.booking.cache;

import com.project.common.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AvailabilityCacheService {
    private final CacheManager cacheManager;

    public void evict(UUID subFieldId, LocalDate date) {
        Cache cache = cacheManager.getCache(CacheNames.AVAILABILITY);
        if (cache != null) {
            cache.evict("availability:" + subFieldId + ":" + date);
        }
    }

    public void evictAll() {
        Cache cache = cacheManager.getCache(CacheNames.AVAILABILITY);
        if (cache != null) {
            cache.clear();
        }
    }
}
