package com.project.booking.repository;

import com.project.booking.entity.SubFieldProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookingSubFieldProjectionRepository extends JpaRepository<SubFieldProjection, UUID> {
}
