package com.project.booking.moderation.entity;

import com.project.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "field_violations", uniqueConstraints = @UniqueConstraint(name = "uk_field_violation_user_field", columnNames = {"user_id", "field_id"}))
@SQLDelete(sql = "UPDATE field_violations SET deleted = true WHERE id = ?::uuid")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldViolation extends BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "field_id", nullable = false)
    private UUID fieldId;

    @Builder.Default
    @Column(name = "violation_count", nullable = false)
    private Integer violationCount = 0;

    @Builder.Default
    @Column(name = "is_banned", nullable = false)
    private Boolean banned = false;

    @Column(name = "ban_date")
    private LocalDateTime banDate;

    @Column(name = "last_violation_date")
    private LocalDateTime lastViolationDate;
}
