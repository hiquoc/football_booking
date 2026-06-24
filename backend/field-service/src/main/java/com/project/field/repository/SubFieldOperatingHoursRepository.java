package com.project.field.repository;

import com.project.field.entity.SubFieldOperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubFieldOperatingHoursRepository extends JpaRepository<SubFieldOperatingHours, UUID> {
    List<SubFieldOperatingHours> findBySubFieldId(UUID subFieldId);

    Optional<SubFieldOperatingHours> findBySubFieldIdAndDayOfWeek(UUID subFieldId, DayOfWeek dayOfWeek);

    void deleteBySubFieldId(UUID subFieldId);
}
