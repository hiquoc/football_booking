package com.project.field.repository;

import com.project.field.entity.SubField;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubFieldRepository extends JpaRepository<SubField, UUID> {
    List<SubField> findByFieldId(UUID fieldId);

    @EntityGraph(attributePaths = {"field", "field.fieldTypes"})
    Optional<SubField> findWithFieldById(UUID id);
}
