package com.project.booking.community.entity;

import com.project.booking.community.enums.CommunityModerationAction;
import com.project.booking.community.enums.CommunityViolationStatus;
import com.project.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "community_user_violations")
@SQLDelete(sql = "UPDATE community_user_violations SET deleted = true WHERE id = ?::uuid")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityUserViolation extends BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private CommunityModerationAction action;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CommunityViolationStatus status;

    @Column(name = "source_post_id")
    private UUID sourcePostId;
}
