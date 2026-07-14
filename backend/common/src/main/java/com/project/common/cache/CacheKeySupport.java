package com.project.common.cache;

import com.project.common.security.UserPrincipal;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.StringJoiner;

public final class CacheKeySupport {
    private CacheKeySupport() {
    }

    public static String fieldSearch(String keyword,
                                     String fieldType,
                                     String subFieldType,
                                     String district,
                                     String provinceCode,
                                     BigDecimal latitude,
                                     BigDecimal longitude,
                                     Double radiusKm,
                                     String sortBy,
                                     String direction,
                                     int page,
                                     int size,
                                     UserPrincipal currentUser) {
        String principal = currentUser == null || currentUser.id() == null
                ? "anonymous"
                : currentUser.role() + ":" + currentUser.id();
        StringJoiner joiner = new StringJoiner("|");
        joiner.add(normalize(keyword));
        joiner.add(normalize(fieldType));
        joiner.add(normalize(subFieldType));
        joiner.add(normalize(district));
        joiner.add(normalize(provinceCode));
        joiner.add(normalize(latitude));
        joiner.add(normalize(longitude));
        joiner.add(normalize(radiusKm));
        joiner.add(normalize(sortBy));
        joiner.add(normalize(direction));
        joiner.add(Integer.toString(page));
        joiner.add(Integer.toString(size));
        joiner.add(principal);
        return "field-search:" + sha256(joiner.toString());
    }

    private static String normalize(Object value) {
        return value == null ? "" : value.toString().trim().toLowerCase();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }
}
