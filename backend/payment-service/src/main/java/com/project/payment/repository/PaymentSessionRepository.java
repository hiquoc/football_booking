package com.project.payment.repository;
import com.project.payment.entity.PaymentSession;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PaymentSessionRepository extends JpaRepository<PaymentSession, String> {}
