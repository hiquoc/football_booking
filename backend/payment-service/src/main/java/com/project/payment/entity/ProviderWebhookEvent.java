package com.project.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="provider_webhook_events") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProviderWebhookEvent {
    @Id @Column(name="event_id", length=255) private String eventId;
    @Column(nullable=false, length=30) private String provider;
    @Column(name="event_type", nullable=false, length=100) private String eventType;
    @Column(name="processed_at", nullable=false) private Instant processedAt;
}
