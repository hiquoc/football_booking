package com.project.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfig {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "booking_fee", nullable = false)
    private Long bookingFee;

    @Column(name = "refund_before_hours", nullable = false)
    private Integer refundBeforeHours;

    @Column(name = "refund_enabled", nullable = false)
    private Boolean refundEnabled;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
