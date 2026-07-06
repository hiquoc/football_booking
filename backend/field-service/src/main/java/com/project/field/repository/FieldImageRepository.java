package com.project.field.repository;

import com.project.field.entity.FieldImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

@Repository
public interface FieldImageRepository extends JpaRepository<FieldImage, Long> {
    List<FieldImage> findByFieldIdAndImageUrlIsNotNull(UUID fieldId);
    List<FieldImage> findByFieldIdAndUploadOwnerIdAndUploadRequestIdOrderByUploadSlotIndex(UUID fieldId, UUID ownerId, UUID requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FieldImage> findByFieldIdAndPublicId(UUID fieldId, String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<FieldImage> findByImageUrlIsNullAndCreatedAtBefore(LocalDateTime cutoff);
}
