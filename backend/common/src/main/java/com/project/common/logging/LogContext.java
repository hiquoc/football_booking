package com.project.common.logging;

import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

public final class LogContext {

    private LogContext() {
    }

    public static String requestIdOrNew(String requestId) {
        return StringUtils.hasText(requestId) ? requestId : UUID.randomUUID().toString();
    }

    public static void putIfPresent(String key, Object value) {
        if (value != null && StringUtils.hasText(value.toString())) {
            MDC.put(key, value.toString());
        }
    }

    public static void putRequestContext(String requestId, String serviceName) {
        putIfPresent(MdcFields.REQUEST_ID, requestId);
        putIfPresent(MdcFields.SERVICE_NAME, serviceName);
    }

    public static void restore(Map<String, String> previousContext) {
        MDC.clear();
        if (previousContext != null) {
            MDC.setContextMap(previousContext);
        }
    }
}
