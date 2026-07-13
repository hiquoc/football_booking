package com.project.payment.config;
import com.project.common.security.SecurityAuditorAware;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
@Configuration @EnableJpaAuditing(auditorAwareRef="auditorProvider")
public class JpaConfig {
    @Bean public AuditorAware<String> auditorProvider() { return new SecurityAuditorAware(); }
}
