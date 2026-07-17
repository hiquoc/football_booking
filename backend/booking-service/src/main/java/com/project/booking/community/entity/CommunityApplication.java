package com.project.booking.community.entity;

import com.project.booking.community.enums.CommunityApplicationStatus;
import com.project.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "community_applications")
@SQLDelete(sql = "UPDATE community_applications SET deleted = true WHERE id = ?::uuid")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityApplication extends BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    @Column(name = "applicant_id", nullable = false)
    private UUID applicantId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CommunityApplicationStatus status;
    @Column(name = "message", length = 1000)
    private String message;
    @Column(name = "applicant_display_name")
    private String applicantDisplayName;
    @Column(name = "applicant_avatar_url", length = 1000)
    private String applicantAvatarUrl;
    @Column(name = "applicant_team_photo_url", length = 1000)
    private String applicantTeamPhotoUrl;
    @Column(name = "applicant_skill_level", length = 40)
    private String applicantSkillLevel;
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;
    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;
}
