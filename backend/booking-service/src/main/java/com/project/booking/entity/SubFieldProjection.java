package com.project.booking.entity;

import com.project.common.enums.SubFieldType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "sub_field_projections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubFieldProjection {

    @Id
    private UUID id;

    @Column(name = "field_id", nullable = false)
    private UUID fieldId;

    @Column(name = "field_name")
    private String fieldName;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sub_field_type", length = 50)
    private SubFieldType subFieldType;

    @Column(name = "minimum_booking_duration_minutes")
    private Integer minimumBookingDurationMinutes;

    @Column(name = "maximum_booking_duration_minutes")
    private Integer maximumBookingDurationMinutes;

    @Column(name = "booking_interval_minutes")
    private Integer bookingIntervalMinutes;
}
