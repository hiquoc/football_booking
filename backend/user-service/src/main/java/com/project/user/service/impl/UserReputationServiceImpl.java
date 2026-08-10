package com.project.user.service.impl;

import com.project.common.cache.CacheNames;
import com.project.common.events.notification.MatchEvaluationSubmittedEvent;
import com.project.common.exception.NotFoundException;
import com.project.user.entity.User;
import com.project.user.entity.UserReputationEvaluation;
import com.project.user.enums.SkillLevel;
import com.project.user.kafka.UserProfileEventPublisher;
import com.project.user.repository.UserRepository;
import com.project.user.repository.UserReputationEvaluationRepository;
import com.project.user.repository.UserReputationSummary;
import com.project.user.service.UserReputationService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserReputationServiceImpl implements UserReputationService {
    private final UserRepository userRepository;
    private final UserReputationEvaluationRepository evaluationRepository;
    private final CacheManager cacheManager;
    private final UserProfileEventPublisher userProfileEventPublisher;

    @Override
    @Transactional
    public void recordEvaluation(MatchEvaluationSubmittedEvent event) {
        User user = userRepository.findForUpdateById(event.evaluatedUserId())
                .orElseThrow(() -> new NotFoundException("User not found with id " + event.evaluatedUserId()));

        UserReputationEvaluation evaluation = evaluationRepository.findBySourceEvaluationId(event.evaluationId())
                .orElseGet(() -> UserReputationEvaluation.builder()
                        .sourceEvaluationId(event.evaluationId())
                        .postId(event.postId())
                        .bookingId(event.bookingId())
                        .evaluatorId(event.evaluatorId())
                        .evaluatedUserId(event.evaluatedUserId())
                        .build());
        evaluation.setArrivedOnTime(event.arrivedOnTime());
        evaluation.setCancelledUnexpectedly(event.cancelledUnexpectedly());
        evaluation.setFairPlay(event.fairPlay());
        evaluation.setWouldPlayAgain(event.wouldPlayAgain());
        evaluation.setSkillLevel(normalizeSkillLevel(event.skillLevel()).name());
        evaluation.setComment(event.comment());
        evaluation.setOccurredAt(event.occurredAt());
        evaluationRepository.save(evaluation);

        UserReputationSummary summary = evaluationRepository.summarize(event.evaluatedUserId());
        user.setOnTimeRate(rate(summary.onTimeCount(), summary.total()));
        user.setNoCancelRate(rate(summary.noCancelCount(), summary.total()));
        user.setFairPlayRate(rate(summary.fairPlayCount(), summary.total()));
        user.setSkillLevel(calculateSkillLevel(evaluationRepository.findReviewedSkillLevels(event.evaluatedUserId()), user.getSkillLevel()));
        evictUser(event.evaluatedUserId());
        userProfileEventPublisher.publishUpdated(user);
    }

    private SkillLevel calculateSkillLevel(List<String> reviewedSkillLevels, SkillLevel fallback) {
        if (reviewedSkillLevels.isEmpty()) {
            return fallback == null ? SkillLevel.AVERAGE : fallback;
        }
        SkillLevel[] levels = SkillLevel.values();
        int total = reviewedSkillLevels.stream()
                .map(this::normalizeSkillLevel)
                .mapToInt(Enum::ordinal)
                .sum();
        int averageIndex = BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(reviewedSkillLevels.size()), 0, RoundingMode.HALF_UP)
                .intValue();
        return levels[Math.max(0, Math.min(levels.length - 1, averageIndex))];
    }

    private SkillLevel normalizeSkillLevel(String skillLevel) {
        if (skillLevel == null || skillLevel.isBlank()) {
            return SkillLevel.AVERAGE;
        }
        return SkillLevel.valueOf(skillLevel.trim().toUpperCase(Locale.ROOT));
    }

    private BigDecimal rate(long count, long total) {
        if (total == 0) {
            return BigDecimal.valueOf(100).setScale(2);
        }
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private void evictUser(UUID userId) {
        var cache = cacheManager.getCache(CacheNames.USER_BY_ID);
        if (cache != null) {
            cache.evict("user:" + userId);
            cache.evict("profile:" + userId);
        }
    }
}
