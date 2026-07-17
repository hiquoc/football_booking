package com.project.booking.community.repository;

import com.project.booking.community.entity.CommunityPost;
import com.project.booking.community.enums.CommunityPostStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, UUID>, JpaSpecificationExecutor<CommunityPost> {
    @Override
    @EntityGraph(attributePaths = "applications")
    Optional<CommunityPost> findById(UUID id);

    boolean existsByBookingIdAndStatusIn(UUID bookingId, Collection<CommunityPostStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE CommunityPost p
            SET p.status = :cancelledStatus,
                p.closedAt = CURRENT_TIMESTAMP
            WHERE p.bookingId = :bookingId
              AND p.status = :openStatus
            """)
    int cancelOpenPostForBooking(
            @Param("bookingId") UUID bookingId,
            @Param("openStatus") CommunityPostStatus openStatus,
            @Param("cancelledStatus") CommunityPostStatus cancelledStatus);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE CommunityPost p
            SET p.status = :closedStatus,
                p.closedAt = CURRENT_TIMESTAMP
            WHERE p.status = :openStatus
              AND (p.bookingDate < :currentDate
                   OR (p.bookingDate = :currentDate AND p.startTime <= :currentTime))
            """)
    int closeStartedOpenPosts(
            @Param("openStatus") CommunityPostStatus openStatus,
            @Param("closedStatus") CommunityPostStatus closedStatus,
            @Param("currentDate") LocalDate currentDate,
            @Param("currentTime") LocalTime currentTime);

    List<CommunityPost> findByStatusAndBookingDateLessThanEqual(CommunityPostStatus status, LocalDate date);

    List<CommunityPost> findByOwnerIdAndStatusIn(UUID ownerId, Collection<CommunityPostStatus> statuses);
}
