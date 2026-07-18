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
    @Column(name="sub_field_price", nullable=false, precision=19, scale=2) private BigDecimal subFieldPrice;
    @Builder.Default
    @Column(name="booking_price", nullable=false) private Long bookingPrice = 0L;
    @Builder.Default
    @Column(name="platform_booking_fee", nullable=false) private Long platformBookingFee = 0L;
}
