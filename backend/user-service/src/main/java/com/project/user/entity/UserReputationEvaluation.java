package com.project.user.entity;

import com.project.common.entity.BaseEntity;
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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_reputation_evaluations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReputationEvaluation extends BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "source_evaluation_id", unique = true)
    private UUID sourceEvaluationId;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "evaluator_id", nullable = false)
    private UUID evaluatorId;

    @Column(name = "evaluated_user_id", nullable = false)
    private UUID evaluatedUserId;

    @Column(name = "arrived_on_time", nullable = false)
    private boolean arrivedOnTime;

    @Column(name = "cancelled_unexpectedly", nullable = false)
    private boolean cancelledUnexpectedly;

    @Column(name = "fair_play", nullable = false)
    private boolean fairPlay;

    @Column(name = "would_play_again", nullable = false)
    private boolean wouldPlayAgain;

    @Column(name = "skill_level", nullable = false, length = 40)
    private String skillLevel;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
