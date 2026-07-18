package com.project.booking.entity;

import com.project.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "user_replicas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReplica extends BaseEntity {
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Builder.Default
    @Column(name = "completed_booking_count", nullable = false)
    private Integer completedBookingCount = 0;

    @Builder.Default
    @Column(name = "total_matches", nullable = false)
    private Integer totalMatches = 0;

    @Builder.Default
    @Column(name = "wins", nullable = false)
    private Integer wins = 0;

    @Builder.Default
    @Column(name = "losses", nullable = false)
    private Integer losses = 0;

    @Builder.Default
    @Column(name = "draws", nullable = false)
    private Integer draws = 0;

    @Builder.Default
    @Column(name = "no_cancel_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal noCancelRate = BigDecimal.valueOf(100);

    @Builder.Default
    @Column(name = "on_time_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal onTimeRate = BigDecimal.valueOf(100);

    @Builder.Default
    @Column(name = "fair_play_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal fairPlayRate = BigDecimal.valueOf(100);
}
