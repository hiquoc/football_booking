package com.project.user.repository;

import com.project.user.entity.UserReputationEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserReputationEvaluationRepository extends JpaRepository<UserReputationEvaluation, UUID> {
    Optional<UserReputationEvaluation> findBySourceEvaluationId(UUID sourceEvaluationId);

    @Query("""
            select new com.project.user.repository.UserReputationSummary(
                count(e),
                coalesce(sum(case when e.arrivedOnTime = true then 1 else 0 end), 0),
                coalesce(sum(case when e.cancelledUnexpectedly = false then 1 else 0 end), 0),
                coalesce(sum(case when e.fairPlay = true then 1 else 0 end), 0)
            )
            from UserReputationEvaluation e
            where e.evaluatedUserId = :userId
            """)
    UserReputationSummary summarize(UUID userId);

    @Query("""
            select e.skillLevel
            from UserReputationEvaluation e
            where e.evaluatedUserId = :userId
              and e.skillLevel is not null
            """)
    List<String> findReviewedSkillLevels(UUID userId);
}
