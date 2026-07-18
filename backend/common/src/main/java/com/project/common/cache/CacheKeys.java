package com.project.common.cache;

public final class CacheKeys {
    public static final String USER = "'user:' + #id";
    public static final String USER_WITH_REQUESTER = "'user:' + #id";
    public static final String FIELD_DETAIL = "'field:' + #id + ':viewer:' + (#userPrincipal == null ? 'anonymous' : #userPrincipal.role() + ':' + #userPrincipal.id())";
    public static final String FIELD_SEARCH = "T(com.project.common.cache.CacheKeySupport).fieldSearch(#keyword, #fieldType, #subFieldType, #district, #provinceCode, #latitude, #longitude, #radiusKm, #sortBy, #direction, #page, #size, #currentUser)";
    public static final String AVAILABILITY = "'availability:' + #subFieldId + ':' + #date";
    public static final String BOOKING_CONFIG = "'current'";

    private CacheKeys() {
    }
}
