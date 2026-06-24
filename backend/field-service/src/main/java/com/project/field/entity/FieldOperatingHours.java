package com.project.field.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "field_operating_hours",
        uniqueConstraints = @UniqueConstraint(columnNames = {"field_id", "day_of_week"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldOperatingHours {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "field_id", nullable = false)
    private UUID fieldId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "closed", nullable = false)
    private Boolean closed;
}
