package com.project.field.repository;

import com.project.field.entity.SubFieldClosure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface FieldClosureRepository extends JpaRepository<SubFieldClosure, UUID> {
    List<SubFieldClosure> findBySubFieldIdOrderByStartDateAsc(UUID subFieldId);

    @Query("""
            select closure
            from SubFieldClosure closure
            where closure.subFieldId in :subFieldIds
              and closure.startDate <= :endDate
              and closure.endDate >= :startDate
            """)
    List<SubFieldClosure> findOverlappingClosures(
            @Param("subFieldIds") Set<UUID> subFieldIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
