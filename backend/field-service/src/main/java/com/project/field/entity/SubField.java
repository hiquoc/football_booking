package com.project.field.entity;

import com.project.common.entity.BaseEntity;
import com.project.common.enums.SubFieldType;
import com.project.field.enums.IndoorOutdoor;
import com.project.field.enums.SurfaceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sub_fields")
@SQLDelete(sql = "UPDATE sub_fields SET deleted = true WHERE id = ?::uuid")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubField extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private Field field;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "indoor_outdoor", length = 20)
    private IndoorOutdoor indoorOutdoor;

    @Enumerated(EnumType.STRING)
    @Column(name = "surface_type", length = 50)
    private SurfaceType surfaceType;

    /** The single bookable configuration; its field type is defined by the enum value. */
    @Enumerated(EnumType.STRING)
    @Column(name = "sub_field_type", length = 50, nullable = false)
    private SubFieldType subFieldType;

    @Builder.Default
    @Column(name = "changing_room", nullable = false)
    private Boolean changingRoom = false;

    @Builder.Default
    @Column(name = "shower", nullable = false)
    private Boolean shower = false;

    @Builder.Default
    @Column(name = "wifi", nullable = false)
    private Boolean wifi = false;

    @Builder.Default
    @Column(name = "air_conditioning", nullable = false)
    private Boolean airConditioning = false;

    @OneToOne(mappedBy = "subField", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private BookingRule bookingRule;

    @OneToMany(mappedBy = "subField", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TimePriceRule> timePriceRules = new ArrayList<>();
}
