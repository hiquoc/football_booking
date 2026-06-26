package com.project.common.kafka;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
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
        try {
            ProducerRecord<String, Object> record = new ProducerRecord<>(
                    topic,
                    null,
                    key,
                    payload,
                    KafkaHeaderUtil.toHeaders(headers));
            kafkaTemplate.send(record).get();
            publishSuccess.increment();
            log.info("Published Kafka event: topic={}, key={}", topic, key);
        } catch (Exception ex) {
            publishFailure.increment();
            throw new IllegalStateException("Failed to publish Kafka event to topic " + topic, ex);
        }
    }

    public void publishDlq(String topic, String key, Object payload, Map<String, String> headers) {
        publish(topic, key, payload, headers);
        dlqPublished.increment();
    }
}
