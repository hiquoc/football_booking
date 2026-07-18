package com.project.user.service.impl;

import com.project.common.cache.CacheNames;
import com.project.common.events.notification.MatchEvaluationSubmittedEvent;
import com.project.common.exception.NotFoundException;
import com.project.user.entity.User;
import com.project.user.entity.UserReputationEvaluation;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserReputationServiceImpl implements UserReputationService {
    private final UserRepository userRepository;
    private final UserReputationEvaluationRepository evaluationRepository;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public void recordEvaluation(MatchEvaluationSubmittedEvent event) {
        User user = userRepository.findForUpdateById(event.evaluatedUserId())
                .orElseThrow(() -> new NotFoundException("User not found with id " + event.evaluatedUserId()));

        evaluationRepository.save(UserReputationEvaluation.builder()
                .postId(event.postId())
                .bookingId(event.bookingId())
                .evaluatorId(event.evaluatorId())
                .evaluatedUserId(event.evaluatedUserId())
                .arrivedOnTime(event.arrivedOnTime())
                .cancelledUnexpectedly(event.cancelledUnexpectedly())
                .fairPlay(event.fairPlay())
                .wouldPlayAgain(event.wouldPlayAgain())
                .comment(event.comment())
                .occurredAt(event.occurredAt())
                .build());

        UserReputationSummary summary = evaluationRepository.summarize(event.evaluatedUserId());
        user.setOnTimeRate(rate(summary.onTimeCount(), summary.total()));
        user.setNoCancelRate(rate(summary.noCancelCount(), summary.total()));
        user.setFairPlayRate(rate(summary.fairPlayCount(), summary.total()));
        evictUser(event.evaluatedUserId());
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
