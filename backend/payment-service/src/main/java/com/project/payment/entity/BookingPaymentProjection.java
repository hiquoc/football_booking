package com.project.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name="booking_payment_projections") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingPaymentProjection {
    @Id @Column(name="booking_id") private UUID bookingId;
    @Column(name="booking_code", nullable=false, length=50) private String bookingCode;
    @Column(name="user_id", nullable=false) private UUID userId;
    @Column(name="user_email") private String userEmail;
    @Column(name="total_amount", nullable=false, precision=19, scale=2) private BigDecimal totalAmount;
}
