package com.project.booking.repository;

import com.project.booking.entity.TimePriceRuleProjection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingTimePriceRuleProjectionRepository extends JpaRepository<TimePriceRuleProjection, UUID> {
    List<TimePriceRuleProjection> findBySubFieldIdOrderByStartTimeAsc(UUID subFieldId);

    void deleteBySubFieldId(UUID subFieldId);
}
