package com.project.booking.moderation.repository;

import com.project.booking.moderation.entity.BookingNoShowReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@Repository
public interface BookingNoShowReportRepository extends JpaRepository<BookingNoShowReport, UUID> {
    boolean existsByBookingId(UUID bookingId);
    Page<BookingNoShowReport> findByFieldIdOrderByCreatedAtDesc(UUID fieldId, Pageable pageable);
}
