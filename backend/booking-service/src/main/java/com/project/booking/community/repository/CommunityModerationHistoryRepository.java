package com.project.booking.community.repository;

import com.project.booking.community.entity.CommunityModerationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommunityModerationHistoryRepository extends JpaRepository<CommunityModerationHistory, UUID> {
    Page<CommunityModerationHistory> findByTargetUserIdOrderByCreatedAtDesc(UUID targetUserId, Pageable pageable);
}
