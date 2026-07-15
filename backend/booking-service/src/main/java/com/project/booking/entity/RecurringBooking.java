package com.project.booking.entity;

import com.project.common.entity.BaseEntity;
import com.project.common.enums.RecurringBookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "recurring_bookings")
@SQLDelete(sql = "UPDATE recurring_bookings SET deleted = true WHERE id = ?::uuid")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringBooking extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "field_id", nullable = false)
    private UUID fieldId;

    @Column(name = "sub_field_id", nullable = false)
    private UUID subFieldId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_field_id", insertable = false, updatable = false)
    private SubFieldProjection subField;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", length = 20, nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RecurringBookingStatus status = RecurringBookingStatus.ACTIVE;

    @Column(name = "next_process_at", nullable = false)
    private LocalDateTime nextProcessAt;
}
