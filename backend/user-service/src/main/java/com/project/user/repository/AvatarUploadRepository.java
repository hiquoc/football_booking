package com.project.user.repository;

import com.project.user.entity.AvatarUpload;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.time.LocalDateTime;
import java.util.*;

public interface AvatarUploadRepository extends JpaRepository<AvatarUpload, Long> {
    Optional<AvatarUpload> findByUserIdAndRequestId(UUID userId, UUID requestId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<AvatarUpload> findByUserIdAndPublicId(UUID userId, String publicId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) List<AvatarUpload> findBySecureUrlIsNullAndCreatedAtBefore(LocalDateTime cutoff);
}
