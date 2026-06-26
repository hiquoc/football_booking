package com.project.common.constants;

public final class GlobalConstants {
    private GlobalConstants() {
        // Prevent instantiation
    }

    public static final String CORRELATION_HEADER_NAME = "X-Correlation-Id";
    
    // Gateway-forwarded headers
    public static final String HEADER_INTERNAL_SECRET = "X-Internal-Gateway-Secret";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_USER_EMAIL = "X-User-Email";

    // Redis OTP constants
    public static final String REDIS_KEY_OTP_CODE_PREFIX = "otp:code:";
    public static final String REDIS_KEY_OTP_COOLDOWN_PREFIX = "otp:cooldown:";
    public static final String REDIS_KEY_OTP_ATTEMPTS_PREFIX = "otp:attempts:";

    // Redis cache constants
    public static final String REDIS_KEY_AVAILABILITY_PREFIX = "booking:availability:";
    public static final String REDIS_KEY_POPULAR_FIELDS = "field:popular";
    public static final String REDIS_KEY_BOOKINGS_QUERY = "booking:query:";
}
