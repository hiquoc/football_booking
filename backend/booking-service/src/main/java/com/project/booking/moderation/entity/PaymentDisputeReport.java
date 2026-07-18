package com.project.booking.moderation.entity;

import com.project.booking.moderation.enums.PaymentDisputeStatus;
import com.project.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payment_dispute_reports")
@SQLDelete(sql = "UPDATE payment_dispute_reports SET deleted = true WHERE id = ?::uuid")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDisputeReport extends BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "field_id", nullable = false)
    private UUID fieldId;

    @Column(name = "reported_user_id", nullable = false)
    private UUID reportedUserId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentDisputeStatus status = PaymentDisputeStatus.PENDING;

    @Column(name = "admin_note", length = 1000)
    private String adminNote;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @ElementCollection
    @CollectionTable(name = "payment_dispute_report_images", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "image_url", nullable = false, length = 1000)
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();
}
