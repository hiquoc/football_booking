package com.project.booking.moderation.repository;

import com.project.booking.moderation.entity.PlatformBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PlatformBanRepository extends JpaRepository<PlatformBan, UUID> {
    boolean existsByUserId(UUID userId);
}
