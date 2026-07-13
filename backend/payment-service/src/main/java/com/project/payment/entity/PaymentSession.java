package com.project.payment.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity @Table(name="payment_sessions") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentSession {
    @Id @Column(name="provider_session_id") private String providerSessionId;
    @Column(name="payment_id", nullable=false) private UUID paymentId;
    @Column(nullable=false) private int attempt;
}
