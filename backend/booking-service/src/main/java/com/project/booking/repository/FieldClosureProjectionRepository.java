package com.project.booking.repository;

import com.project.booking.entity.SubFieldClosureProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface FieldClosureProjectionRepository extends JpaRepository<SubFieldClosureProjection, UUID> {
    boolean existsBySubFieldIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID subFieldId,
            LocalDate date,
            LocalDate sameDate
    );
}
