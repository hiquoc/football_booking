package com.project.booking.moderation.repository;

import com.project.booking.moderation.entity.PaymentDisputeReport;
import com.project.booking.moderation.enums.PaymentDisputeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentDisputeReportRepository extends JpaRepository<PaymentDisputeReport, UUID> {
    boolean existsByBookingIdAndOwnerId(UUID bookingId, UUID ownerId);
    Page<PaymentDisputeReport> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId, Pageable pageable);
    Page<PaymentDisputeReport> findByStatusOrderByCreatedAtDesc(PaymentDisputeStatus status, Pageable pageable);
    Page<PaymentDisputeReport> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
