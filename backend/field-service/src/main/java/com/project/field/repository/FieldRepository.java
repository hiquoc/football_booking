package com.project.field.repository;

import com.project.field.entity.Field;
import com.project.field.enums.FieldStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FieldRepository extends JpaRepository<Field, UUID>, JpaSpecificationExecutor<Field> {
    Page<Field> findByOwnerId(UUID ownerId, Pageable pageable);
    Page<Field> findByStatus(FieldStatus status, Pageable pageable);
    Page<Field> findByStatusAndActiveTrue(FieldStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"fieldTypes"})
    Optional<Field> findWithDetailsById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Field f where f.id = :id")
    Optional<Field> findByIdForUpdate(UUID id);
}
