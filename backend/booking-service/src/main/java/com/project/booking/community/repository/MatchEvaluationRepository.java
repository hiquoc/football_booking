package com.project.booking.community.repository;

import com.project.booking.community.entity.MatchEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchEvaluationRepository extends JpaRepository<MatchEvaluation, UUID> {
    boolean existsByPostIdAndEvaluatorIdAndEvaluatedUserId(UUID postId, UUID evaluatorId, UUID evaluatedUserId);
    Optional<MatchEvaluation> findByPostIdAndEvaluatorIdAndEvaluatedUserId(UUID postId, UUID evaluatorId, UUID evaluatedUserId);
    List<MatchEvaluation> findByPostIdAndEvaluatorId(UUID postId, UUID evaluatorId);
}
