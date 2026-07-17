package com.project.booking.community.repository;

import com.project.booking.community.entity.MatchEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchEvaluationRepository extends JpaRepository<MatchEvaluation, UUID> {
    boolean existsByPostIdAndEvaluatorIdAndEvaluatedUserId(UUID postId, UUID evaluatorId, UUID evaluatedUserId);
}
