package com.project.field.repository;

import com.project.field.entity.FieldOperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FieldOperatingHoursRepository extends JpaRepository<FieldOperatingHours, UUID> {
    List<FieldOperatingHours> findByFieldId(UUID fieldId);

    Optional<FieldOperatingHours> findByFieldIdAndDayOfWeek(UUID fieldId, DayOfWeek dayOfWeek);
}
