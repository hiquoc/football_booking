package com.project.field.repository;

import com.project.field.entity.Field;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FieldRepository extends JpaRepository<Field, UUID>, JpaSpecificationExecutor<Field> {
    List<Field> findByOwnerId(UUID ownerId);
}
