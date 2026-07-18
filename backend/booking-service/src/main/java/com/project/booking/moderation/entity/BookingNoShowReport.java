package com.project.booking.moderation.entity;

import com.project.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "booking_no_show_reports", uniqueConstraints = @UniqueConstraint(name = "uk_no_show_report_booking", columnNames = "booking_id"))
@SQLDelete(sql = "UPDATE booking_no_show_reports SET deleted = true WHERE id = ?::uuid")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingNoShowReport extends BaseEntity {
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
}
