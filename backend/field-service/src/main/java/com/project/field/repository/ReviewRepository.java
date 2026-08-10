package com.project.field.repository;

import com.project.field.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    @Query("""
            SELECT r
            FROM Review r
            WHERE r.field.id = :fieldId
              AND r.deleted = false
            ORDER BY r.createdAt DESC
            """)
    Page<Review> findByFieldIdOrderByCreatedAtDesc(UUID fieldId, Pageable pageable);

    Optional<Review> findFirstByFieldIdAndUserIdAndDeletedFalseOrderByUpdatedAtDesc(UUID fieldId, UUID userId);
}
