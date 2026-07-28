package com.project.field.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "sub_field_operating_hours",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sub_field_id", "day_of_week"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubFieldOperatingHours {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "sub_field_id", nullable = false)
    private UUID subFieldId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "closed", nullable = false)
    private Boolean closed;

    @Builder.Default
    @Column(name = "open_24_hours", nullable = false)
    private Boolean open24Hours = false;
}
