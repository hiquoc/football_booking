package com.project.booking.repository;

import com.project.booking.entity.FieldOperatingHoursProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FieldOperatingHoursProjectionRepository extends JpaRepository<FieldOperatingHoursProjection, UUID> {
    List<FieldOperatingHoursProjection> findByFieldId(UUID fieldId);

    Optional<FieldOperatingHoursProjection> findByFieldIdAndDayOfWeek(UUID fieldId, DayOfWeek dayOfWeek);
}
