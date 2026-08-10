package com.project.booking.moderation.repository;

import com.project.booking.moderation.entity.ModerationAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ModerationAuditLogRepository extends JpaRepository<ModerationAuditLog, UUID> {
    Page<ModerationAuditLog> findByFieldIdOrderByCreatedAtDesc(UUID fieldId, Pageable pageable);
    Page<ModerationAuditLog> findByTargetUserIdOrderByCreatedAtDesc(UUID targetUserId, Pageable pageable);
}
