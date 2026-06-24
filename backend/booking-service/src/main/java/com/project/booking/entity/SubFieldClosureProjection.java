package com.project.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sub_field_closure_projections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubFieldClosureProjection {

    @Id
    private UUID id;

    @Column(name = "sub_field_id", nullable = false)
    private UUID subFieldId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "reason")
    private String reason;
}
