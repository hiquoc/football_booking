package com.project.booking.community.entity;

import com.project.booking.community.enums.CommunityPostStatus;
import com.project.booking.community.enums.CommunityPostType;
import com.project.common.entity.BaseEntity;
import com.project.common.enums.SubFieldType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "community_posts")
@SQLDelete(sql = "UPDATE community_posts SET deleted = true WHERE id = ?::uuid")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPost extends BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;
    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false, length = 40)
    private CommunityPostType postType;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CommunityPostStatus status;
    @Column(name = "title", nullable = false, length = 120)
    private String title;
    @Column(name = "description", length = 2000)
    private String description;
    @Column(name = "skill_level", nullable = false, length = 40)
    private String skillLevel;
    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;
    @Column(name = "players_needed")
    private Integer playersNeeded;
    @Builder.Default
    @Column(name = "accepted_players_count", nullable = false)
    private Integer acceptedPlayersCount = 0;

    @Column(name = "booking_code", nullable = false, length = 50)
    private String bookingCode;
    @Column(name = "field_id")
    private UUID fieldId;
    @Column(name = "field_owner_id")
    private UUID fieldOwnerId;
    @Column(name = "field_name")
    private String fieldName;
    @Column(name = "sub_field_id", nullable = false)
    private UUID subFieldId;
    @Column(name = "sub_field_name")
    private String subFieldName;
    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", length = 50)
    private SubFieldType fieldType;
    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "owner_display_name")
    private String ownerDisplayName;
    @Column(name = "owner_avatar_url", length = 1000)
    private String ownerAvatarUrl;
    @Column(name = "owner_team_photo_url", length = 1000)
    private String ownerTeamPhotoUrl;
    @Column(name = "location_text")
    private String locationText;
    @Column(name = "matched_application_id")
    private UUID matchedApplicationId;
    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;
    @Column(name = "hidden_reason", length = 500)
    private String hiddenReason;

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<CommunityApplication> applications = new ArrayList<>();
}
