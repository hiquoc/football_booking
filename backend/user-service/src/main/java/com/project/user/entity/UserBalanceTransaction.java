package com.project.user.entity;

import com.project.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "user_balance_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBalanceTransaction extends BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "operation_key", nullable = false, unique = true, length = 120)
    private String operationKey;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "reason", nullable = false, length = 80)
    private String reason;
}
