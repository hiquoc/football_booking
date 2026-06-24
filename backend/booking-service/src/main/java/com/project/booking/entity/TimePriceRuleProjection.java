package com.project.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "time_price_rule_projections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimePriceRuleProjection {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "sub_field_id", nullable = false)
    private UUID subFieldId;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "hourly_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal hourlyPrice;
}
