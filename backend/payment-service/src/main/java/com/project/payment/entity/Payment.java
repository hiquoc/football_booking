package com.project.payment.entity;

import com.project.common.entity.BaseEntity;
import com.project.payment.enums.PaymentProvider;
import com.project.payment.enums.PaymentPurpose;
import com.project.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.util.UUID;
import java.time.Instant;

@Entity @Table(name="payments") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Payment extends BaseEntity {
    @Id @GeneratedValue @UuidGenerator private UUID id;
    @Column(name="booking_id") private UUID bookingId;
    @Column(name="user_id", nullable=false) private UUID userId;
    @Column(name="provider", nullable=false, length=30) @Enumerated(EnumType.STRING) private PaymentProvider provider;
    @Builder.Default
    @Column(name="purpose", nullable=false, length=30) @Enumerated(EnumType.STRING) private PaymentPurpose purpose = PaymentPurpose.WALLET_TOP_UP;
    @Column(name="stripe_session_id", unique=true) private String providerSessionId;
    @Column(name="payment_intent_id", unique=true) private String providerPaymentId;
    @Column(nullable=false, precision=19, scale=2) private BigDecimal amount;
    @Column(nullable=false, length=3) private String currency;
    @Column(nullable=false, length=20) @Enumerated(EnumType.STRING) private PaymentStatus status;
    @Column(name="failure_reason", length=500) private String failureReason;
    @Column(name="checkout_attempt", nullable=false) private int checkoutAttempt;
    @Column(name="expires_at") private Instant expiresAt;
    @Version private long version;
}
