package com.project.booking.moderation.repository;

import com.project.booking.moderation.entity.BookingNoShowReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BookingNoShowReportRepository extends JpaRepository<BookingNoShowReport, UUID> {
    boolean existsByBookingId(UUID bookingId);
}
