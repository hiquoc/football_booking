package com.project.booking.entity;

import com.project.booking.enums.WinningTeam;
import com.project.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "match_results", uniqueConstraints = {
        @UniqueConstraint(name = "uk_match_results_booking", columnNames = "booking_id")
})
@SQLDelete(sql = "UPDATE match_results SET deleted = true WHERE id = ?::uuid")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResult extends BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "winning_team", nullable = false, length = 20)
    private WinningTeam winningTeam;

    @Column(name = "team_a_percentage", nullable = false)
    private Integer teamAPercentage;

    @Column(name = "team_b_percentage", nullable = false)
    private Integer teamBPercentage;

    @Column(name = "team_a_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal teamAAmount;

    @Column(name = "team_b_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal teamBAmount;

    @Column(name = "submitted_by", nullable = false)
    private UUID submittedBy;
}
