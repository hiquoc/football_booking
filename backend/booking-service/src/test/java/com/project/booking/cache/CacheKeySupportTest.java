package com.project.booking.cache;

import com.project.common.cache.CacheKeySupport;
import com.project.common.security.UserPrincipal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheKeySupportTest {

    @Test
    void fieldSearchKeyIsStableForEquivalentParameters() {
        UserPrincipal user = new UserPrincipal(UUID.randomUUID(), "client@example.com", "CLIENT");

        String first = CacheKeySupport.fieldSearch("FOOTBALL", "FIVE_A_SIDE", "District 1", "HCM",
                new BigDecimal("10.123"), new BigDecimal("106.456"), 5.0, "rating", "DESC", 0, 20, user);
        String second = CacheKeySupport.fieldSearch(" football ", "five_a_side", "district 1", "hcm",
                new BigDecimal("10.123"), new BigDecimal("106.456"), 5.0, "rating", "desc", 0, 20, user);

        assertEquals(first, second);
        assertTrue(first.startsWith("field-search:"));
    }

    @Test
    void fieldSearchKeyChangesWhenSearchParameterChanges() {
        String first = CacheKeySupport.fieldSearch("FOOTBALL", null, null, null,
                null, null, null, null, null, 0, 20, null);
        String second = CacheKeySupport.fieldSearch("TENNIS", null, null, null,
                null, null, null, null, null, 0, 20, null);

        assertNotEquals(first, second);
    }
}
