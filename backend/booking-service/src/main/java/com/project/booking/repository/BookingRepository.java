package com.project.booking.repository;

import com.project.common.enums.BookingStatus;
import com.project.booking.entity.Booking;
import com.project.common.enums.BookingCancelledBy;
import com.project.common.enums.BookingPaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

        @Override
        @EntityGraph(attributePaths = "subField")
        Optional<Booking> findById(UUID id);

        @Query("""
                    SELECT COUNT(b) > 0
                    FROM Booking b
                    WHERE b.subFieldId = :subFieldId
                      AND b.startDateTime < :endDateTime
                      AND b.endDateTime > :startDateTime
                      AND b.status IN :reservingStatuses
                """)
        boolean existsConflictingBookings(
                UUID subFieldId,
                LocalDateTime startDateTime,
                LocalDateTime endDateTime,
                Collection<BookingStatus> reservingStatuses);

        @Query("""
                    SELECT COUNT(b) > 0
                    FROM Booking b
                    WHERE b.subFieldId = :subFieldId
                      AND b.startDateTime < :endDateTime
                      AND b.endDateTime > :startDateTime
                      AND b.status IN :reservingStatuses
                      AND (b.sourceRecurringBookingId IS NULL OR b.sourceRecurringBookingId <> :sourceRecurringBookingId)
                """)
        boolean existsConflictingBookingsExcludingSource(
                UUID subFieldId,
                LocalDateTime startDateTime,
                LocalDateTime endDateTime,
                Collection<BookingStatus> reservingStatuses,
                UUID sourceRecurringBookingId);

        default boolean existsConflictingBookings(
                UUID subFieldId,
                LocalDate bookingDate,
                LocalTime startTime,
                LocalTime endTime,
                Collection<BookingStatus> reservingStatuses) {
                LocalDateTime startDateTime = LocalDateTime.of(bookingDate, startTime);
                LocalDateTime endDateTime = LocalDateTime.of(bookingDate, endTime);
                if (!endTime.isAfter(startTime)) {
                        endDateTime = endDateTime.plusDays(1);
                }
                return existsConflictingBookings(subFieldId, startDateTime, endDateTime, reservingStatuses);
        }

        default boolean existsConflictingBookings(
                UUID subFieldId,
                LocalDate bookingDate,
                LocalTime startTime,
                LocalTime endTime,
                Collection<BookingStatus> reservingStatuses,
                UUID sourceRecurringBookingId) {
                if (sourceRecurringBookingId == null) {
                        return existsConflictingBookings(subFieldId, bookingDate, startTime, endTime, reservingStatuses);
                }
                LocalDateTime startDateTime = LocalDateTime.of(bookingDate, startTime);
                LocalDateTime endDateTime = LocalDateTime.of(bookingDate, endTime);
                if (!endTime.isAfter(startTime)) {
                        endDateTime = endDateTime.plusDays(1);
                }
                return existsConflictingBookingsExcludingSource(
                        subFieldId, startDateTime, endDateTime, reservingStatuses, sourceRecurringBookingId);
        }

        boolean existsBySubFieldIdInAndBookingDateBetweenAndStatusIn(
                Collection<UUID> subFieldIds,
                LocalDate startDate,
                LocalDate endDate,
                Collection<BookingStatus> statuses);

        @Query("""
                    SELECT COUNT(b) > 0
                    FROM Booking b
                    WHERE b.clientId = :clientId
                      AND b.subField.fieldId = :fieldId
                      AND b.status = :status
                """)
        boolean existsCompletedBookingAtField(
                @Param("clientId") UUID clientId,
                @Param("fieldId") UUID fieldId,
                @Param("status") BookingStatus status);

        boolean existsBySourceRecurringBookingIdAndStartDateTime(UUID sourceRecurringBookingId, LocalDateTime startDateTime);

        @EntityGraph(attributePaths = "subField")
        Optional<Booking> findFirstBySourceRecurringBookingIdOrderByStartDateTimeAsc(UUID sourceRecurringBookingId);

        @EntityGraph(attributePaths = "subField")
        Optional<Booking> findFirstBySourceRecurringBookingIdOrderByStartDateTimeDesc(UUID sourceRecurringBookingId);

        @Query("""
                    SELECT COUNT(b) > 0
                    FROM Booking b
                    WHERE b.sourceRecurringBookingId = :sourceRecurringBookingId
                      AND b.startDateTime >= :dayStart
                      AND b.startDateTime < :dayEnd
                """)
        boolean existsBySourceRecurringBookingIdOnDate(
                @Param("sourceRecurringBookingId") UUID sourceRecurringBookingId,
                @Param("dayStart") LocalDateTime dayStart,
                @Param("dayEnd") LocalDateTime dayEnd);

        default boolean existsBySourceRecurringBookingIdAndBookingDate(UUID sourceRecurringBookingId, LocalDate bookingDate) {
                return existsBySourceRecurringBookingIdOnDate(
                        sourceRecurringBookingId,
                        bookingDate.atStartOfDay(),
                        bookingDate.plusDays(1).atStartOfDay());
        }

        boolean existsByOwnerIdAndSubFieldFieldId(UUID ownerId, UUID fieldId);

        @Query("""
                    SELECT b
                    FROM Booking b
                    WHERE b.subFieldId = :subFieldId
                      AND b.startDateTime < :windowEnd
                      AND b.endDateTime > :windowStart
                      AND b.status IN :reservingStatuses
                    ORDER BY b.startDateTime ASC
                """)
        List<Booking> findOverlappingBookings(
                @Param("subFieldId") UUID subFieldId,
                @Param("windowStart") LocalDateTime windowStart,
                @Param("windowEnd") LocalDateTime windowEnd,
                @Param("reservingStatuses") Collection<BookingStatus> reservingStatuses);

        default List<Booking> findBySubFieldIdAndBookingDateAndStatusInOrderByStartTimeAsc(
                UUID subFieldId,
                LocalDate bookingDate,
                Collection<BookingStatus> reservingStatuses) {
                return findOverlappingBookings(
                        subFieldId,
                        bookingDate.atStartOfDay(),
                        bookingDate.plusDays(1).atStartOfDay(),
                        reservingStatuses);
        }

        @EntityGraph(attributePaths = "subField")
        Page<Booking> findByClientId(UUID clientId, Pageable pageable);

        @EntityGraph(attributePaths = "subField")
        @Query("""
                    SELECT b
                    FROM Booking b
                    WHERE b.clientId = :clientId
                      AND (CAST(:bookingDateStart AS timestamp) IS NULL OR (b.startDateTime < :bookingDateEnd AND b.endDateTime > :bookingDateStart))
                      AND (CAST(:status AS string) IS NULL OR b.status = :status)
                """)
        Page<Booking> findClientBookings(
                @Param("clientId") UUID clientId,
                @Param("bookingDateStart") LocalDateTime bookingDateStart,
                @Param("bookingDateEnd") LocalDateTime bookingDateEnd,
                @Param("status") BookingStatus status,
                Pageable pageable);

        @EntityGraph(attributePaths = "subField")
        Page<Booking> findByOwnerId(UUID ownerId, Pageable pageable);

        @EntityGraph(attributePaths = "subField")
        @Query("""
                    SELECT b
                    FROM Booking b
                    WHERE b.ownerId = :ownerId
                      AND (CAST(:bookingDateStart AS timestamp) IS NULL OR (b.startDateTime < :bookingDateEnd AND b.endDateTime > :bookingDateStart))
                      AND (CAST(:subFieldId AS uuid) IS NULL OR b.subFieldId = :subFieldId)
                      AND (CAST(:status AS string) IS NULL OR b.status = :status)
                    ORDER BY b.startDateTime ASC
                """)
        Page<Booking> findOwnerBookings(
                @Param("ownerId") UUID ownerId,
                @Param("bookingDateStart") LocalDateTime bookingDateStart,
                @Param("bookingDateEnd") LocalDateTime bookingDateEnd,
                @Param("subFieldId") UUID subFieldId,
                @Param("status") BookingStatus status,
                Pageable pageable);

        @EntityGraph(attributePaths = "subField")
        @Query("""
                    SELECT b
                    FROM Booking b
                    WHERE b.subField.fieldId IN :fieldIds
                      AND (CAST(:bookingDateStart AS timestamp) IS NULL OR (b.startDateTime < :bookingDateEnd AND b.endDateTime > :bookingDateStart))
                      AND (CAST(:subFieldId AS uuid) IS NULL OR b.subFieldId = :subFieldId)
                      AND (CAST(:status AS string) IS NULL OR b.status = :status)
                    ORDER BY b.startDateTime ASC
                """)
        Page<Booking> findEmployeeManagedBookings(
                @Param("fieldIds") Collection<UUID> fieldIds,
                @Param("bookingDateStart") LocalDateTime bookingDateStart,
                @Param("bookingDateEnd") LocalDateTime bookingDateEnd,
                @Param("subFieldId") UUID subFieldId,
                @Param("status") BookingStatus status,
                Pageable pageable);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
                    UPDATE Booking b
                    SET b.status = :confirmedStatus,
                        b.paymentStatus = :paidStatus
                    WHERE b.id = :bookingId
                      AND b.clientId = :clientId
                      AND b.status = :pendingStatus
                """)
        int confirmPendingBooking(
                @Param("bookingId") UUID bookingId,
                @Param("clientId") UUID clientId,
                @Param("pendingStatus") BookingStatus pendingStatus,
                @Param("confirmedStatus") BookingStatus confirmedStatus,
                @Param("paidStatus") BookingPaymentStatus paidStatus);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
                    UPDATE Booking b
                    SET b.status = :confirmedStatus,
                        b.paymentStatus = :paidStatus
                    WHERE b.id = :bookingId
                      AND b.status = :pendingStatus
                """)
        int confirmPendingBookingFromPayment(@Param("bookingId") UUID bookingId,
                @Param("pendingStatus") BookingStatus pendingStatus,
                @Param("confirmedStatus") BookingStatus confirmedStatus,
                @Param("paidStatus") BookingPaymentStatus paidStatus);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
                    UPDATE Booking b
                    SET b.paymentStatus = :refundedStatus
                    WHERE b.id = :bookingId
                      AND b.paymentStatus <> :refundedStatus
                """)
        int markPaymentRefunded(
                @Param("bookingId") UUID bookingId,
                @Param("refundedStatus") BookingPaymentStatus refundedStatus);

        @EntityGraph(attributePaths = "subField")
        Optional<Booking> findFirstByClientIdAndStatusOrderByCreatedAtAsc(UUID clientId, BookingStatus status);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
                    UPDATE Booking b
                    SET b.status = :expiredStatus,
                        b.cancellationReason = :reason,
                        b.cancelledAt = :cancelledAt,
                        b.cancelledBy = :cancelledBy,
                        b.paymentStatus = CASE
                            WHEN b.paymentStatus = :paidStatus THEN b.paymentStatus
                            WHEN b.paymentStatus = :refundedStatus THEN b.paymentStatus
                            ELSE :failedStatus
                        END
                    WHERE b.id = :bookingId
                      AND b.clientId = :clientId
                      AND b.status IN :cancellableStatuses
                """)
        int cancelClientBooking(
                @Param("bookingId") UUID bookingId,
                @Param("clientId") UUID clientId,
                @Param("cancellableStatuses") Collection<BookingStatus> cancellableStatuses,
                @Param("expiredStatus") BookingStatus expiredStatus,
                @Param("reason") String reason,
                @Param("cancelledAt") LocalDateTime cancelledAt,
                @Param("cancelledBy") BookingCancelledBy cancelledBy,
                @Param("paidStatus") BookingPaymentStatus paidStatus,
                @Param("refundedStatus") BookingPaymentStatus refundedStatus,
                @Param("failedStatus") BookingPaymentStatus failedStatus);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
                    UPDATE Booking b
                    SET b.status = :cancelledStatus,
                        b.cancellationReason = :reason,
                        b.cancelledAt = :cancelledAt,
                        b.cancelledBy = :cancelledBy,
                        b.paymentStatus = CASE
                            WHEN b.paymentStatus = :paidStatus THEN b.paymentStatus
                            WHEN b.paymentStatus = :refundedStatus THEN b.paymentStatus
                            ELSE :failedStatus
                        END
                    WHERE b.id = :bookingId
                      AND b.ownerId = :ownerId
                      AND b.status IN :cancellableStatuses
                """)
        int cancelOwnerBooking(
                @Param("bookingId") UUID bookingId,
                @Param("ownerId") UUID ownerId,
                @Param("cancellableStatuses") Collection<BookingStatus> cancellableStatuses,
                @Param("cancelledStatus") BookingStatus cancelledStatus,
                @Param("reason") String reason,
                @Param("cancelledAt") LocalDateTime cancelledAt,
                @Param("cancelledBy") BookingCancelledBy cancelledBy,
                @Param("paidStatus") BookingPaymentStatus paidStatus,
                @Param("refundedStatus") BookingPaymentStatus refundedStatus,
                @Param("failedStatus") BookingPaymentStatus failedStatus);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
                    UPDATE Booking b
                    SET b.status = :cancelledStatus,
                        b.cancellationReason = :reason,
                        b.cancelledAt = :cancelledAt,
                        b.cancelledBy = :cancelledBy,
                        b.paymentStatus = CASE
                            WHEN b.paymentStatus = :paidStatus THEN b.paymentStatus
                            WHEN b.paymentStatus = :refundedStatus THEN b.paymentStatus
                            ELSE :failedStatus
                        END
                    WHERE b.id = :bookingId
                      AND b.status IN :cancellableStatuses
                """)
        int cancelManagerBooking(
                @Param("bookingId") UUID bookingId,
                @Param("cancellableStatuses") Collection<BookingStatus> cancellableStatuses,
                @Param("cancelledStatus") BookingStatus cancelledStatus,
                @Param("reason") String reason,
                @Param("cancelledAt") LocalDateTime cancelledAt,
                @Param("cancelledBy") BookingCancelledBy cancelledBy,
                @Param("paidStatus") BookingPaymentStatus paidStatus,
                @Param("refundedStatus") BookingPaymentStatus refundedStatus,
                @Param("failedStatus") BookingPaymentStatus failedStatus);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
                    UPDATE Booking b
                    SET b.status = :cancelledStatus,
                        b.cancellationReason = :reason,
                        b.cancelledAt = :cancelledAt,
                        b.cancelledBy = :cancelledBy,
                        b.paymentStatus = CASE
                            WHEN b.paymentStatus = :paidStatus THEN b.paymentStatus
                            WHEN b.paymentStatus = :refundedStatus THEN b.paymentStatus
                            ELSE :failedStatus
                        END
                    WHERE b.status = :pendingStatus
                      AND b.paymentExpiresAt <= :expiresBefore
                """)
        int expirePendingBookings(
                @Param("pendingStatus") BookingStatus pendingStatus,
                @Param("cancelledStatus") BookingStatus cancelledStatus,
                @Param("expiresBefore") LocalDateTime expiresBefore,
                @Param("reason") String reason,
                @Param("cancelledAt") LocalDateTime cancelledAt,
                @Param("cancelledBy") BookingCancelledBy cancelledBy,
                @Param("paidStatus") BookingPaymentStatus paidStatus,
                @Param("refundedStatus") BookingPaymentStatus refundedStatus,
                @Param("failedStatus") BookingPaymentStatus failedStatus);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
                    UPDATE Booking b
                    SET b.status = :completedStatus
                    WHERE b.status = :confirmedStatus
                      AND b.endDateTime <= :now
                """)
        int completeConfirmedBookings(
                @Param("confirmedStatus") BookingStatus confirmedStatus,
                @Param("completedStatus") BookingStatus completedStatus,
                @Param("now") LocalDateTime now);

        @EntityGraph(attributePaths = "subField")
        @Query("""
                    SELECT b
                    FROM Booking b
                    WHERE b.status = :confirmedStatus
                      AND b.endDateTime <= :now
                """)
        List<Booking> findFinishedConfirmedBookings(
                @Param("confirmedStatus") BookingStatus confirmedStatus,
                @Param("now") LocalDateTime now);

        default List<Booking> findFinishedConfirmedBookings(
                BookingStatus confirmedStatus,
                LocalDate currentDate,
                LocalTime currentTime) {
                return findFinishedConfirmedBookings(confirmedStatus, LocalDateTime.of(currentDate, currentTime));
        }
}
