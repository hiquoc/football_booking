package com.project.booking.repository;

import com.project.booking.entity.BookingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingConfigRepository extends JpaRepository<BookingConfig, UUID> {
    Optional<BookingConfig> findByActiveTrue();
}
