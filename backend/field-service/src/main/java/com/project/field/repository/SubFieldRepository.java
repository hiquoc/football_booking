package com.project.field.repository;

import com.project.field.entity.SubField;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubFieldRepository extends JpaRepository<SubField, UUID> {
    List<SubField> findByFieldId(UUID fieldId);

    @EntityGraph(attributePaths = {"field", "field.fieldTypes"})
    Optional<SubField> findWithFieldById(UUID id);

    @EntityGraph(attributePaths = {"field"})
    @Query("""
            SELECT s
            FROM SubField s
            JOIN s.field f
            WHERE s.active = true
            AND (
                COALESCE(:search, '') = ''
                OR LOWER(s.name) LIKE CONCAT('%', LOWER(:search), '%')
                OR LOWER(f.name) LIKE CONCAT('%', LOWER(:search), '%')
                OR LOWER(CAST(s.subFieldType AS string)) LIKE CONCAT('%', LOWER(:search), '%')
            )
            ORDER BY f.name, s.name
            """)
    List<SubField> findFilterOptions(@Param("search") String search);
}
