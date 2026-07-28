package com.project.booking.repository;

import com.project.booking.entity.RecurringBooking;
import com.project.common.enums.RecurringBookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
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

    @EntityGraph(attributePaths = "subField")
    Optional<RecurringBooking> findFirstByUserIdAndSubFieldIdAndStartTimeAndEndTimeAndStartDateAndEndDateAndIntervalDaysAndStatus(
            UUID userId,
            UUID subFieldId,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate startDate,
            LocalDate endDate,
            Integer intervalDays,
            RecurringBookingStatus status);

    @Query("""
                SELECT r
                FROM RecurringBooking r
                WHERE r.status = :status
                  AND r.userId = :userId
                  AND (:excludeId IS NULL OR r.id <> :excludeId)
                  AND r.startDate <= :endDate
                  AND r.endDate >= :startDate
                  AND r.startTime < :endTime
                  AND r.endTime > :startTime
            """)
    List<RecurringBooking> findUserOverlapCandidates(
            @Param("userId") UUID userId,
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
                  AND r.startDate <= :endDate
                  AND r.endDate >= :startDate
                  AND r.startTime < :endTime
                  AND r.endTime > :startTime
            """)
    List<RecurringBooking> findSubFieldOverlapCandidates(
            @Param("subFieldId") UUID subFieldId,
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
                  AND r.startDate <= :date
                  AND r.endDate >= :date
                  AND r.startTime < :endTime
                  AND r.endTime > :startTime
                ORDER BY r.startTime ASC
            """)
    List<RecurringBooking> findActiveConflictCandidatesForDate(
            @Param("subFieldId") UUID subFieldId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("status") RecurringBookingStatus status,
            @Param("excludeId") UUID excludeId);

    default List<RecurringBooking> findActiveConflictsForDate(
            UUID subFieldId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            RecurringBookingStatus status,
            UUID excludeId) {
        return findActiveConflictCandidatesForDate(subFieldId, date, startTime, endTime, status, excludeId)
                .stream()
                .filter(recurringBooking -> occursOn(recurringBooking, date))
                .toList();
    }

    @Query("""
                SELECT r
                FROM RecurringBooking r
                WHERE r.status = :status
                  AND r.subFieldId = :subFieldId
                  AND r.startDate <= :date
                  AND r.endDate >= :date
                ORDER BY r.startTime ASC
            """)
    List<RecurringBooking> findActiveReservationCandidatesForDate(
            @Param("subFieldId") UUID subFieldId,
            @Param("date") LocalDate date,
            @Param("status") RecurringBookingStatus status);

    default List<RecurringBooking> findActiveReservationsForDate(
            UUID subFieldId,
            LocalDate date,
            RecurringBookingStatus status) {
        return findActiveReservationCandidatesForDate(subFieldId, date, status)
                .stream()
                .filter(recurringBooking -> occursOn(recurringBooking, date))
                .toList();
    }

    default boolean overlapsAnyGeneratedOccurrence(
            List<RecurringBooking> candidates,
            LocalDate startDate,
            LocalDate endDate,
            int intervalDays) {
        for (LocalDate current = startDate; !current.isAfter(endDate); current = current.plusDays(intervalDays)) {
            LocalDate occurrenceDate = current;
            if (candidates.stream().anyMatch(candidate -> occursOn(candidate, occurrenceDate))) {
                return true;
            }
        }
        return false;
    }

    private static boolean occursOn(RecurringBooking recurringBooking, LocalDate date) {
        long days = ChronoUnit.DAYS.between(recurringBooking.getStartDate(), date);
        return days >= 0 && days % recurringBooking.getIntervalDays() == 0;
    }
}
