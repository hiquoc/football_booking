package com.project.booking.moderation.repository;

import com.project.booking.moderation.entity.FieldViolation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FieldViolationRepository extends JpaRepository<FieldViolation, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FieldViolation> findForUpdateByUserIdAndFieldId(UUID userId, UUID fieldId);
    Optional<FieldViolation> findByUserIdAndFieldId(UUID userId, UUID fieldId);
    boolean existsByUserIdAndFieldIdAndBannedTrue(UUID userId, UUID fieldId);
    long countByUserIdAndBannedTrue(UUID userId);
    Page<FieldViolation> findByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);
    Page<FieldViolation> findByFieldIdAndBannedTrueOrderByBanDateDesc(UUID fieldId, Pageable pageable);
    Page<FieldViolation> findByFieldIdOrderByUpdatedAtDesc(UUID fieldId, Pageable pageable);
    List<FieldViolation> findAllByBannedTrue();
}
