package com.project.booking.repository;

import com.project.booking.entity.SubFieldProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface BookingSubFieldProjectionRepository extends JpaRepository<SubFieldProjection, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SubFieldProjection s SET s.hasRecurring = :hasRecurring WHERE s.id = :subFieldId")
    int updateHasRecurring(@Param("subFieldId") UUID subFieldId, @Param("hasRecurring") boolean hasRecurring);
}
