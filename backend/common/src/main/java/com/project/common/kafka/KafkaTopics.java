package com.project.common.kafka;

public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static String dlqTopic(String topic, String suffix) {
        return topic + suffix;
    }
}
