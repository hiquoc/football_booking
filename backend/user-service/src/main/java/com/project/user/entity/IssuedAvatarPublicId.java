package com.project.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "issued_avatar_public_ids")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class IssuedAvatarPublicId {
    @Id @Column(name = "public_id", length = 255) private String publicId;
    @Column(name = "issued_at", nullable = false) private LocalDateTime issuedAt;
}
