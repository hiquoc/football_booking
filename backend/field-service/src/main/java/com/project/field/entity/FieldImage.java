package com.project.field.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "field_images")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private Field field;

    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
}
