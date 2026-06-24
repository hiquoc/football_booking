package com.project.field.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sub_field_closures")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubFieldClosure {

    @Id
    @GeneratedValue
    @UuidGenerator
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
