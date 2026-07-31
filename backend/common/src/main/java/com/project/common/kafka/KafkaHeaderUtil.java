package com.project.common.kafka;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public final class KafkaHeaderUtil {

    public static final String EVENT_ID = "eventId";
    public static final String EVENT_TYPE = "eventType";
    public static final String AGGREGATE_ID = "aggregateId";
    public static final String REQUEST_ID = "requestId";

    private KafkaHeaderUtil() {
    }

    public static Headers toHeaders(Map<String, String> headers) {
        RecordHeaders recordHeaders = new RecordHeaders();
        if (headers != null) {
            headers.forEach((key, value) -> {
                if (value != null) {
                    recordHeaders.add(key, value.getBytes(StandardCharsets.UTF_8));
                }
            });
        }
        return recordHeaders;
    }

    public static Optional<String> header(ConsumerRecord<String, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        if (header == null || header.value() == null) {
            return Optional.empty();
        }
        return Optional.of(new String(header.value(), StandardCharsets.UTF_8));
    }
}
