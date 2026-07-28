package com.project.payment.repository;
import com.project.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByBookingId(UUID bookingId);
    Optional<Payment> findTopByBookingIdOrderByCreatedAtDesc(UUID bookingId);
    Optional<Payment> findByProviderSessionId(String providerSessionId);
}
