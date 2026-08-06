package com.project.booking.repository;

import com.project.booking.entity.RecurringBooking;
import com.project.common.enums.RecurringBookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    List<RecurringBooking> findByStatusOrderByNextProcessAtAsc(RecurringBookingStatus status);

    @Query(value = """
            SELECT *
            FROM recurring_bookings
            WHERE id = :id
              AND status = :status
              AND deleted = false
              AND (next_process_at IS NULL OR next_process_at <= :now)
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<RecurringBooking> lockDueById(
            @Param("id") UUID id,
            @Param("status") String status,
            @Param("now") LocalDateTime now);

    boolean existsBySubFieldIdAndStatus(UUID subFieldId, RecurringBookingStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
                UPDATE RecurringBooking r
                SET r.status = :newStatus
                WHERE r.id = :id
                  AND r.status = :currentStatus
            """)
    int updateStatus(
            @Param("id") UUID id,
            @Param("currentStatus") RecurringBookingStatus currentStatus,
            @Param("newStatus") RecurringBookingStatus newStatus);

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
            """)
    List<RecurringBooking> findUserOverlapCandidates(
            @Param("userId") UUID userId,
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
            """)
    List<RecurringBooking> findSubFieldOverlapCandidates(
            @Param("subFieldId") UUID subFieldId,
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
                ORDER BY r.startTime ASC
            """)
    List<RecurringBooking> findActiveConflictCandidatesForDate(
            @Param("subFieldId") UUID subFieldId,
            @Param("date") LocalDate date,
            @Param("status") RecurringBookingStatus status,
            @Param("excludeId") UUID excludeId);

    default List<RecurringBooking> findActiveConflictsForDate(
            UUID subFieldId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            RecurringBookingStatus status,
            UUID excludeId) {
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.concat(
                                findActiveConflictCandidatesForDate(subFieldId, date.minusDays(1), status, excludeId).stream(),
                                findActiveConflictCandidatesForDate(subFieldId, date, status, excludeId).stream()),
                        findActiveConflictCandidatesForDate(subFieldId, date.plusDays(1), status, excludeId).stream())
                .distinct()
                .filter(recurringBooking -> overlapsOccurrenceOnDate(recurringBooking, date, startTime, endTime))
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
            LocalTime startTime,
            LocalTime endTime,
            LocalDate startDate,
            LocalDate endDate,
            int intervalDays) {
        for (LocalDate current = startDate; !current.isAfter(endDate); current = current.plusDays(intervalDays)) {
            LocalDate occurrenceDate = current;
            if (candidates.stream().anyMatch(candidate ->
                    overlapsOccurrenceOnDate(candidate, occurrenceDate, startTime, endTime))) {
                return true;
            }
        }
        return false;
    }

    private static boolean occursOn(RecurringBooking recurringBooking, LocalDate date) {
        long days = ChronoUnit.DAYS.between(recurringBooking.getStartDate(), date);
        return days >= 0 && days % recurringBooking.getIntervalDays() == 0;
    }

    private static boolean overlapsOccurrenceOnDate(
            RecurringBooking recurringBooking,
            LocalDate requestedDate,
            LocalTime requestedStartTime,
            LocalTime requestedEndTime) {
        LocalDateTime requestedStart = LocalDateTime.of(requestedDate, requestedStartTime);
        LocalDateTime requestedEnd = occurrenceEnd(requestedStart, requestedEndTime);
        return overlapsCandidateOccurrence(recurringBooking, requestedStart, requestedEnd, requestedDate.minusDays(1))
                || overlapsCandidateOccurrence(recurringBooking, requestedStart, requestedEnd, requestedDate)
                || overlapsCandidateOccurrence(recurringBooking, requestedStart, requestedEnd, requestedDate.plusDays(1));
    }

    private static boolean overlapsCandidateOccurrence(
            RecurringBooking recurringBooking,
            LocalDateTime requestedStart,
            LocalDateTime requestedEnd,
            LocalDate candidateDate) {
        if (!occursOn(recurringBooking, candidateDate)) {
            return false;
        }
        LocalDateTime candidateStart = LocalDateTime.of(candidateDate, recurringBooking.getStartTime());
        LocalDateTime candidateEnd = occurrenceEnd(candidateStart, recurringBooking.getEndTime());
        return candidateStart.isBefore(requestedEnd) && candidateEnd.isAfter(requestedStart);
    }

    private static LocalDateTime occurrenceEnd(LocalDateTime startDateTime, LocalTime endTime) {
        LocalDateTime endDateTime = LocalDateTime.of(startDateTime.toLocalDate(), endTime);
        if (!endDateTime.isAfter(startDateTime)) {
            endDateTime = endDateTime.plusDays(1);
        }
        return endDateTime;
    }
}
