package com.project.booking.repository;

import com.project.booking.entity.RecurringBooking;
import com.project.common.enums.RecurringBookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringBookingRepository extends JpaRepository<RecurringBooking, UUID> {

    @Override
    @EntityGraph(attributePaths = "subField")
    Optional<RecurringBooking> findById(UUID id);

    @EntityGraph(attributePaths = "subField")
    Page<RecurringBooking> findByUserIdAndStatus(UUID userId, RecurringBookingStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "subField")
    Page<RecurringBooking> findByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = "subField")
    Page<RecurringBooking> findBySubFieldOwnerIdAndStatus(UUID ownerId, RecurringBookingStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "subField")
    Page<RecurringBooking> findBySubFieldOwnerId(UUID ownerId, Pageable pageable);

    @EntityGraph(attributePaths = "subField")
    Page<RecurringBooking> findByStatus(RecurringBookingStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "subField")
    Page<RecurringBooking> findAll(Pageable pageable);

    List<RecurringBooking> findByStatusAndNextProcessAtLessThanEqualOrderByNextProcessAtAsc(
            RecurringBookingStatus status,
            LocalDateTime nextProcessAt);

    boolean existsBySubFieldIdAndStatus(UUID subFieldId, RecurringBookingStatus status);

    @Query("""
                SELECT COUNT(r) > 0
                FROM RecurringBooking r
                WHERE r.status = :status
                  AND r.userId = :userId
                  AND (:excludeId IS NULL OR r.id <> :excludeId)
                  AND r.dayOfWeek = :dayOfWeek
                  AND r.startDate <= :endDate
                  AND r.endDate >= :startDate
                  AND r.startTime < :endTime
                  AND r.endTime > :startTime
            """)
    boolean existsUserOverlap(
            @Param("userId") UUID userId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") RecurringBookingStatus status,
            @Param("excludeId") UUID excludeId);

    @Query("""
                SELECT COUNT(r) > 0
                FROM RecurringBooking r
                WHERE r.status = :status
                  AND r.subFieldId = :subFieldId
                  AND (:excludeId IS NULL OR r.id <> :excludeId)
                  AND r.dayOfWeek = :dayOfWeek
                  AND r.startDate <= :endDate
                  AND r.endDate >= :startDate
                  AND r.startTime < :endTime
                  AND r.endTime > :startTime
            """)
    boolean existsSubFieldOverlap(
            @Param("subFieldId") UUID subFieldId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") RecurringBookingStatus status,
            @Param("excludeId") UUID excludeId);

    @Query("""
                SELECT r
                FROM RecurringBooking r
                WHERE r.status = :status
                  AND r.subFieldId = :subFieldId
                  AND (:excludeId IS NULL OR r.id <> :excludeId)
                  AND r.dayOfWeek = :dayOfWeek
                  AND r.startDate <= :date
                  AND r.endDate >= :date
                  AND r.startTime < :endTime
                  AND r.endTime > :startTime
                ORDER BY r.startTime ASC
            """)
    List<RecurringBooking> findActiveConflictsForDate(
            @Param("subFieldId") UUID subFieldId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("status") RecurringBookingStatus status,
            @Param("excludeId") UUID excludeId);

    @Query("""
                SELECT r
                FROM RecurringBooking r
                WHERE r.status = :status
                  AND r.subFieldId = :subFieldId
                  AND r.dayOfWeek = :dayOfWeek
                  AND r.startDate <= :date
                  AND r.endDate >= :date
                ORDER BY r.startTime ASC
            """)
    List<RecurringBooking> findActiveReservationsForDate(
            @Param("subFieldId") UUID subFieldId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("date") LocalDate date,
            @Param("status") RecurringBookingStatus status);
}
