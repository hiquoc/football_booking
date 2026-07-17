package com.project.booking.community.repository;

import com.project.booking.community.entity.CommunityUserViolation;
import com.project.booking.community.enums.CommunityModerationAction;
import com.project.booking.community.enums.CommunityViolationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

public interface CommunityUserViolationRepository extends JpaRepository<CommunityUserViolation, UUID> {
    long countByUserIdAndStatusIn(UUID userId, Collection<CommunityViolationStatus> statuses);

    Page<CommunityUserViolation> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndStatus(UUID userId, CommunityViolationStatus status);

    boolean existsByUserIdAndStatus(UUID userId, CommunityViolationStatus status);

    boolean existsByUserIdAndActionAndStatus(
            UUID userId,
            CommunityModerationAction action,
            CommunityViolationStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE CommunityUserViolation v
            SET v.status = :expiredStatus
            WHERE v.status = :activeStatus
              AND v.expireAt IS NOT NULL
              AND v.expireAt <= :now
            """)
    int expireTemporaryViolations(
            @Param("activeStatus") CommunityViolationStatus activeStatus,
            @Param("expiredStatus") CommunityViolationStatus expiredStatus,
            @Param("now") LocalDateTime now);
}
