package com.project.field.repository;

import com.project.field.entity.TimePriceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimePriceRuleRepository extends JpaRepository<TimePriceRule, Long> {
    List<TimePriceRule> findBySubFieldId(UUID subFieldId);
}
