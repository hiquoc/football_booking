package com.project.common.kafka;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Counter publishSuccess;
    private final Counter publishFailure;
    private final Counter dlqPublished;

    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.publishSuccess = meterRegistry.counter("kafka.publish.success");
        this.publishFailure = meterRegistry.counter("kafka.publish.failure");
        this.dlqPublished = meterRegistry.counter("kafka.dlq.published");
    }

    public void publish(String topic, String key, Object payload, Map<String, String> headers) {
        long startedAt = System.nanoTime();
        String eventType = headers == null ? null : headers.get(KafkaHeaderUtil.EVENT_TYPE);
        String requestId = headers == null ? null : headers.get(KafkaHeaderUtil.REQUEST_ID);
        String aggregateId = headers == null ? null : headers.get(KafkaHeaderUtil.AGGREGATE_ID);
        log.info("kafka_producer_send_started eventType={} topic={} key={} requestId={} aggregateId={}",
                eventType, topic, key, requestId, aggregateId);
        try {
            ProducerRecord<String, Object> record = new ProducerRecord<>(
                    topic,
                    null,
                    key,
                    payload,
                    KafkaHeaderUtil.toHeaders(headers));
            SendResult<String, Object> result = kafkaTemplate.send(record).get();
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            publishSuccess.increment();
            log.info("kafka_producer_send_succeeded eventType={} topic={} key={} requestId={} aggregateId={} partition={} offset={} elapsedTimeMs={}",
                    eventType,
                    topic,
                    key,
                    requestId,
                    aggregateId,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    elapsedMs);
        } catch (Exception ex) {
            publishFailure.increment();
            log.error("kafka_producer_send_failed topic={} key={} requestId={} eventType={} aggregateId={}",
                    topic, key, requestId, eventType, aggregateId, ex);
            throw new IllegalStateException("Failed to publish Kafka event to topic " + topic, ex);
        }
    }

    public void publishDlq(String topic, String key, Object payload, Map<String, String> headers) {
        publish(topic, key, payload, headers);
        dlqPublished.increment();
        log.error("kafka_dlq_published dlqTopic={} originalTopic={} eventType={} requestId={} aggregateId={}",
                topic,
                headers == null ? null : headers.get("originalTopic"),
                headers == null ? null : headers.get(KafkaHeaderUtil.EVENT_TYPE),
                headers == null ? null : headers.get(KafkaHeaderUtil.REQUEST_ID),
                headers == null ? null : headers.get(KafkaHeaderUtil.AGGREGATE_ID));
    }
}
