package com.project.booking.repository;

import com.project.booking.entity.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchResultRepository extends JpaRepository<MatchResult, UUID> {
    Optional<MatchResult> findByBookingId(UUID bookingId);

    List<MatchResult> findByBookingIdIn(Collection<UUID> bookingIds);
}
