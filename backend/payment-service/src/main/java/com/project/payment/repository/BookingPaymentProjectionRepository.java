package com.project.payment.repository;
import com.project.payment.entity.BookingPaymentProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface BookingPaymentProjectionRepository extends JpaRepository<BookingPaymentProjection, UUID> {}
