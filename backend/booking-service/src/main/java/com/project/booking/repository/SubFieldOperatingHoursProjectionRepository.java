package com.project.booking.repository;

import com.project.booking.entity.SubFieldOperatingHoursProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubFieldOperatingHoursProjectionRepository extends JpaRepository<SubFieldOperatingHoursProjection, UUID> {
    List<SubFieldOperatingHoursProjection> findBySubFieldId(UUID subFieldId);

    Optional<SubFieldOperatingHoursProjection> findBySubFieldIdAndDayOfWeek(UUID subFieldId, DayOfWeek dayOfWeek);
}
