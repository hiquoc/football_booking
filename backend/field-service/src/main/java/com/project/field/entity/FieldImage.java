package com.project.field.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "public_id", length = 255, unique = true)
    private String publicId;

    @Column(name = "upload_owner_id")
    private UUID uploadOwnerId;

    @Column(name = "upload_request_id")
    private UUID uploadRequestId;

    @Column(name = "upload_slot_index")
    private Integer uploadSlotIndex;

    @Column(name = "upload_timestamp")
    private Long uploadTimestamp;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "asset_version")
    private Long assetVersion;

    @Column(name = "image_format", length = 32)
    private String imageFormat;

    private Integer width;
    private Integer height;

    @Column(name = "byte_size")
    private Long byteSize;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
}
