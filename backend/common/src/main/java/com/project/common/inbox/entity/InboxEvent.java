package com.project.common.inbox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inbox_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_inbox_event_consumer", columnNames = {"event_id", "consumer_group"}),
        indexes = {
                @Index(name = "idx_inbox_status", columnList = "status"),
                @Index(name = "idx_inbox_next_retry_at", columnList = "next_retry_at")
        })
public class InboxEvent {

    @Id
    private UUID id;

    @Column(nullable = false, name = "event_id")
    private String eventId;

    @Column(nullable = false, name = "consumer_group")
    private String consumerGroup;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false, name = "kafka_partition")
    private int partition;

    @Column(nullable = false, name = "kafka_offset")
    private long offset;

    @Column(nullable = false, name = "payload_type")
    private String payloadType;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InboxEventStatus status;

    @Column(nullable = false, name = "retry_count")
    private int retryCount;

    @Column(nullable = false, name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(nullable = false, updatable = false, name = "received_at")
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = InboxEventStatus.RECEIVED;
        }
        if (receivedAt == null) {
            receivedAt = now;
        }
        if (nextRetryAt == null) {
            nextRetryAt = now;
        }
    }
}
