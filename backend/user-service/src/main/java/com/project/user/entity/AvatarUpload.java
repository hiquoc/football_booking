package com.project.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "avatar_uploads", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "request_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AvatarUpload {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "request_id", nullable = false) private UUID requestId;
    @Column(name = "public_id", nullable = false, unique = true, length = 255) private String publicId;
    @Column(name = "upload_timestamp", nullable = false) private Long uploadTimestamp;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "confirmed_at") private LocalDateTime confirmedAt;
    @Column(name = "secure_url", length = 500) private String secureUrl;
    @Column(name = "asset_version") private Long assetVersion;
    @Column(name = "image_format", length = 32) private String imageFormat;
    private Integer width;
    private Integer height;
    @Column(name = "byte_size") private Long byteSize;
}
