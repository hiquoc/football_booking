package com.project.payment.repository;
import com.project.payment.entity.ProviderWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProviderWebhookEventRepository extends JpaRepository<ProviderWebhookEvent, String> {}
