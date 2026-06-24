package com.project.field.repository;

import com.project.field.entity.FieldImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FieldImageRepository extends JpaRepository<FieldImage, Long> {
    List<FieldImage> findByFieldId(UUID fieldId);
}
