package com.project.field.repository;

import com.project.field.entity.FieldEmployeeAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FieldEmployeeAssignmentRepository extends JpaRepository<FieldEmployeeAssignment, UUID> {
    boolean existsByFieldIdAndEmployeeId(UUID fieldId, UUID employeeId);
    Optional<FieldEmployeeAssignment> findByFieldIdAndEmployeeId(UUID fieldId, UUID employeeId);
    List<FieldEmployeeAssignment> findByFieldIdOrderByCreatedAtAsc(UUID fieldId);
    List<FieldEmployeeAssignment> findByEmployeeIdOrderByCreatedAtAsc(UUID employeeId);
    boolean existsByEmployeeIdAndFieldId(UUID employeeId, UUID fieldId);
    boolean existsByEmployeeId(UUID employeeId);
}
