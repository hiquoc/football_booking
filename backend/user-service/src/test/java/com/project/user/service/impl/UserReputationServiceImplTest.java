package com.project.user.service.impl;

import com.project.common.events.notification.MatchEvaluationSubmittedEvent;
import com.project.user.entity.User;
import com.project.user.enums.SkillLevel;
import com.project.user.kafka.UserProfileEventPublisher;
import com.project.user.repository.UserRepository;
import com.project.user.repository.UserReputationEvaluationRepository;
import com.project.user.repository.UserReputationSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserReputationServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserReputationEvaluationRepository evaluationRepository;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private UserProfileEventPublisher userProfileEventPublisher;

    @Test
    void recordEvaluationRecalculatesSkillLevelAndPublishesProfileUpdate() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .fullName("Player")
                .skillLevel(SkillLevel.AVERAGE)
                .build();
        MatchEvaluationSubmittedEvent event = new MatchEvaluationSubmittedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                userId,
                true,
                false,
                true,
                true,
                "PRO",
                "Strong opponent",
                Instant.now());
        UserReputationServiceImpl service = new UserReputationServiceImpl(
                userRepository,
                evaluationRepository,
                cacheManager,
                userProfileEventPublisher);

        when(userRepository.findForUpdateById(userId)).thenReturn(Optional.of(user));
        when(evaluationRepository.summarize(userId)).thenReturn(new UserReputationSummary(2, 2, 1, 2));
        when(evaluationRepository.findReviewedSkillLevels(userId)).thenReturn(List.of("GOOD", "PRO"));

        service.recordEvaluation(event);

        assertEquals(SkillLevel.SEMI_PRO, user.getSkillLevel());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userProfileEventPublisher).publishUpdated(captor.capture());
        assertEquals(userId, captor.getValue().getId());
        verify(evaluationRepository).save(any());
    }
}
