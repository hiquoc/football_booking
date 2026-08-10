package com.project.booking.moderation.repository;

import com.project.booking.moderation.entity.PlatformBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface PlatformBanRepository extends JpaRepository<PlatformBan, UUID> {
    boolean existsByUserId(UUID userId);

    @Modifying
    @Query(value = """
            INSERT INTO platform_bans (user_id, reason, banned_at, created_at, updated_at, deleted)
            VALUES (:userId, :reason, :bannedAt, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, false)
            ON CONFLICT (user_id) DO UPDATE
            SET reason = EXCLUDED.reason,
                banned_at = EXCLUDED.banned_at,
                updated_at = CURRENT_TIMESTAMP,
                deleted = false
            """, nativeQuery = true)
    int upsertActiveBan(@Param("userId") UUID userId, @Param("reason") String reason, @Param("bannedAt") LocalDateTime bannedAt);

    @Modifying
    @Query(value = "UPDATE platform_bans SET deleted = true, updated_at = CURRENT_TIMESTAMP WHERE user_id = :userId AND deleted = false", nativeQuery = true)
    int deleteByUserId(UUID userId);
}
