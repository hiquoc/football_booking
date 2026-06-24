package com.project.field.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "booking_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_field_id", nullable = false, unique = true)
    private SubField subField;

    @Column(name = "minimum_booking_duration_minutes")
    private Integer minimumBookingDurationMinutes;

    @Column(name = "maximum_booking_duration_minutes")
    private Integer maximumBookingDurationMinutes;

    @Column(name = "booking_interval_minutes")
    private Integer bookingIntervalMinutes;
}
