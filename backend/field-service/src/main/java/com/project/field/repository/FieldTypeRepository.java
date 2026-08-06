package com.project.field.repository;

import com.project.common.enums.SportType;
import com.project.field.entity.FieldType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FieldTypeRepository extends JpaRepository<FieldType, Long> {
    Optional<FieldType> findByName(SportType name);
}
