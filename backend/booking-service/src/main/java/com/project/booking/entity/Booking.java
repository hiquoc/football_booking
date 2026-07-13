package com.project.booking.entity;

import com.project.common.enums.BookingCancelledBy;
import com.project.common.enums.BookingStatus;
import com.project.common.enums.PaymentMethod;
import com.project.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@SQLDelete(sql = "UPDATE bookings SET deleted = true WHERE id = ?::uuid")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "booking_code", length = 50, unique = true, nullable = false)
    private String bookingCode;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "sub_field_id", nullable = false)
    private UUID subFieldId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_field_id", insertable = false, updatable = false)
    private SubFieldProjection subField;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "price_per_hour", precision = 10, scale = 2, nullable = false)
    private BigDecimal pricePerHour;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 30, nullable = false)
    private PaymentMethod paymentMethod = PaymentMethod.STRIPE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "note")
    private String note;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancelled_by", length = 20)
    private BookingCancelledBy cancelledBy;
}
