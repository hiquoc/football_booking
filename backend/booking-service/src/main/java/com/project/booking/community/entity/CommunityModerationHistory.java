package com.project.booking.community.entity;

import com.project.booking.community.enums.CommunityModerationAction;
import com.project.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "community_moderation_history")
@SQLDelete(sql = "UPDATE community_moderation_history SET deleted = true WHERE id = ?::uuid")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityModerationHistory extends BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "target_user_id", nullable = false)
    private UUID targetUserId;

    @Column(name = "target_post_id")
    private UUID targetPostId;

    @Column(name = "moderator_id", nullable = false)
    private UUID moderatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 60)
    private CommunityModerationAction action;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "note", length = 1000)
    private String note;
}
