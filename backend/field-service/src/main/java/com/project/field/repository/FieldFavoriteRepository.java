package com.project.field.repository;

import com.project.field.entity.FieldFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FieldFavoriteRepository extends JpaRepository<FieldFavorite, UUID> {
    boolean existsByUserIdAndFieldId(UUID userId, UUID fieldId);
    Optional<FieldFavorite> findByUserIdAndFieldId(UUID userId, UUID fieldId);

    @Query("select ff.field.id from FieldFavorite ff where ff.userId = :userId and ff.field.id in :fieldIds")
    List<UUID> findFavoriteFieldIds(UUID userId, List<UUID> fieldIds);

    @EntityGraph(attributePaths = {"field", "field.fieldTypes", "field.images"})
    List<FieldFavorite> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"field", "field.fieldTypes", "field.images"})
    Page<FieldFavorite> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
