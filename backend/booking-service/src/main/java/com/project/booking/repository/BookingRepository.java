package com.project.booking.repository;

import com.project.common.enums.BookingStatus;
import com.project.booking.entity.Booking;
import com.project.common.enums.BookingCancelledBy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

        @Query("""
                    SELECT COUNT(b) > 0
                    FROM Booking b
                    WHERE b.subFieldId = :subFieldId
                      AND b.bookingDate = :bookingDate
                      AND b.startTime < :endTime
                      AND b.endTime > :startTime
                      AND b.status IN :reservingStatuses
                """)
        boolean existsConflictingBookings(
                UUID subFieldId,
                LocalDate bookingDate,
                LocalTime startTime,
                LocalTime endTime,
                Collection<BookingStatus> reservingStatuses);

        List<Booking> findBySubFieldIdAndBookingDateAndStatusInOrderByStartTimeAsc(
                        UUID subFieldId,
                        LocalDate bookingDate,
                        Collection<BookingStatus> reservingStatuses);

        Page<Booking> findByClientId(UUID clientId, Pageable pageable);

        Page<Booking> findByOwnerId(UUID ownerId, Pageable pageable);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
                    UPDATE Booking b
                    SET b.status = :confirmedStatus
                    WHERE b.id = :bookingId
                      AND b.clientId = :clientId
                      AND b.status = :pendingStatus
                """)
        int confirmPendingBooking(
                @Param("bookingId") UUID bookingId,
                @Param("clientId") UUID clientId,
                @Param("pendingStatus") BookingStatus pendingStatus,
                @Param("confirmedStatus") BookingStatus confirmedStatus);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
                    UPDATE Booking b
                    SET b.status = :cancelledStatus,
                        b.cancellationReason = :reason,
                        b.cancelledAt = :cancelledAt,
                        b.cancelledBy = :cancelledBy
                    WHERE b.id = :bookingId
                      AND b.clientId = :clientId
                      AND b.status IN :cancellableStatuses
                """)
        int cancelClientBooking(
                @Param("bookingId") UUID bookingId,
                @Param("clientId") UUID clientId,
                @Param("cancellableStatuses") Collection<BookingStatus> cancellableStatuses,
                @Param("cancelledStatus") BookingStatus cancelledStatus,
                @Param("reason") String reason,
                @Param("cancelledAt") LocalDateTime cancelledAt,
                @Param("cancelledBy") BookingCancelledBy cancelledBy);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
                    UPDATE Booking b
                    SET b.status = :cancelledStatus,
                        b.cancellationReason = :reason,
                        b.cancelledAt = :cancelledAt,
                        b.cancelledBy = :cancelledBy
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
                @Param("cancelledBy") BookingCancelledBy cancelledBy);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
                    UPDATE Booking b
                    SET b.status = :expiredStatus,
                        b.cancellationReason = :reason,
                        b.cancelledAt = :cancelledAt,
                        b.cancelledBy = :cancelledBy
                    WHERE b.status = :pendingStatus
                      AND b.createdAt <= :expiresBefore
                """)
        int expirePendingBookings(
                @Param("pendingStatus") BookingStatus pendingStatus,
                @Param("expiredStatus") BookingStatus expiredStatus,
                @Param("expiresBefore") LocalDateTime expiresBefore,
                @Param("reason") String reason,
                @Param("cancelledAt") LocalDateTime cancelledAt,
                @Param("cancelledBy") BookingCancelledBy cancelledBy);
}
