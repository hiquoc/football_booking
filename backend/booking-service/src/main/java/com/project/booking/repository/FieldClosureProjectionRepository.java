package com.project.booking.repository;

import com.project.booking.entity.SubFieldClosureProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface FieldClosureProjectionRepository extends JpaRepository<SubFieldClosureProjection, UUID> {
    boolean existsBySubFieldIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID subFieldId,
            LocalDate date,
            LocalDate sameDate
    );

    @Query("""
                SELECT COUNT(c) > 0
                FROM SubFieldClosureProjection c
                WHERE c.subFieldId = :subFieldId
                  AND c.startDate <= :endDate
                  AND c.endDate >= :startDate
            """)
    boolean existsOverlappingDateRange(
            @Param("subFieldId") UUID subFieldId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
