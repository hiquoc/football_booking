package com.project.booking.moderation.entity;

import com.project.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "moderation_audit_logs")
@SQLDelete(sql = "UPDATE moderation_audit_logs SET deleted = true WHERE id = ?::uuid")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationAuditLog extends BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(name = "field_id")
    private UUID fieldId;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "details", length = 1000)
    private String details;
}
