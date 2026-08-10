package com.project.booking.community.entity;

import com.project.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "match_evaluations")
@SQLDelete(sql = "UPDATE match_evaluations SET deleted = true WHERE id = ?::uuid")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchEvaluation extends BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;
    @Column(name = "post_id", nullable = false)
    private UUID postId;
    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;
    @Column(name = "evaluator_id", nullable = false)
    private UUID evaluatorId;
    @Column(name = "evaluated_user_id", nullable = false)
    private UUID evaluatedUserId;
    @Column(name = "arrived_on_time", nullable = false)
    private Boolean arrivedOnTime;
    @Column(name = "cancelled_unexpectedly", nullable = false)
    private Boolean cancelledUnexpectedly;
    @Column(name = "fair_play", nullable = false)
    private Boolean fairPlay;
    @Column(name = "would_play_again", nullable = false)
    private Boolean wouldPlayAgain;
    @Column(name = "skill_level", nullable = false, length = 40)
    private String skillLevel;
    @Column(name = "comment", length = 1000)
    private String comment;
}
