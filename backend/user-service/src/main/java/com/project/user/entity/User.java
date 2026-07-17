package com.project.user.entity;

import com.project.common.enums.UserType;
import com.project.common.entity.BaseEntity;
import com.project.user.enums.SkillLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phone_number", length = 20, unique = true)
    private String phoneNumber;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "avatar_public_id", length = 255, unique = true)
    private String avatarPublicId;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "team_photo_url")
    private String teamPhotoUrl;

    @Column(name = "team_photo_public_id", length = 255, unique = true)
    private String teamPhotoPublicId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", length = 30, nullable = false)
    private SkillLevel skillLevel = SkillLevel.AVERAGE;

    @Builder.Default
    @Column(name = "total_matches", nullable = false)
    private Integer totalMatches = 0;

    @Builder.Default
    @Column(name = "wins", nullable = false)
    private Integer wins = 0;

    @Builder.Default
    @Column(name = "draws", nullable = false)
    private Integer draws = 0;

    @Builder.Default
    @Column(name = "losses", nullable = false)
    private Integer losses = 0;

    @Builder.Default
    @Column(name = "no_cancel_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal noCancelRate = BigDecimal.valueOf(100);

    @Builder.Default
    @Column(name = "on_time_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal onTimeRate = BigDecimal.valueOf(100);

    @Builder.Default
    @Column(name = "fair_play_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal fairPlayRate = BigDecimal.valueOf(100);

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", length = 20, nullable = false)
    private UserType userType;

    @Column(name = "social_provider", length = 20)
    private String socialProvider;

    @Column(name = "social_provider_id", length = 100)
    private String socialProviderId;

    @Builder.Default
    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE"; // ACTIVE, BLOCKED, DELETED

    @Builder.Default
    @Column(name = "balance", nullable = false)
    private Long balance = 0L;
}
