package com.project.common.kafka;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class KafkaHeaderUtil {

    public static final String EVENT_ID = "eventId";

    private KafkaHeaderUtil() {
    }

    public static Headers toHeaders(Map<String, String> headers) {
        RecordHeaders recordHeaders = new RecordHeaders();
        headers.forEach((key, value) -> recordHeaders.add(key, value.getBytes(StandardCharsets.UTF_8)));
        return recordHeaders;
    }
}
