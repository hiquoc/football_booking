package com.project.booking.cache;

import com.project.common.cache.CacheNames;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AvailabilityCacheServiceTest {

    @Test
    void evictsAvailabilityBySubFieldAndDateKey() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        AvailabilityCacheService service = new AvailabilityCacheService(cacheManager);
        UUID subFieldId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);
        when(cacheManager.getCache(CacheNames.AVAILABILITY)).thenReturn(cache);

        service.evict(subFieldId, date);

        verify(cache).evict("availability:" + subFieldId + ":" + date);
    }

    @Test
    void evictAllClearsAvailabilityCache() {
        CacheManager cacheManager = mock(CacheManager.class);
        Cache cache = mock(Cache.class);
        AvailabilityCacheService service = new AvailabilityCacheService(cacheManager);
        when(cacheManager.getCache(CacheNames.AVAILABILITY)).thenReturn(cache);

        service.evictAll();

        verify(cache).clear();
    }
}
