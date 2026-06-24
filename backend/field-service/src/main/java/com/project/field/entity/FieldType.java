package com.project.field.entity;

import com.project.common.entity.BaseEntity;
import com.project.common.enums.SportType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "field_types")
@SQLDelete(sql = "UPDATE field_types SET deleted = true WHERE id = ?::uuid")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "sport_type", length = 30, nullable = false, unique = true)
    private SportType name;

    @Column(name = "default_booking_duration_minutes", nullable = false)
    private Integer defaultBookingDurationMinutes;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
