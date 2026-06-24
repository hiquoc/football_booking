package com.project.field.repository;

import com.project.field.entity.BookingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRuleRepository extends JpaRepository<BookingRule, Long> {
}
