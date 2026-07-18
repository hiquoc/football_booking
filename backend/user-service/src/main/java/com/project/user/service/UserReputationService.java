package com.project.user.service;

import com.project.common.events.notification.MatchEvaluationSubmittedEvent;

public interface UserReputationService {
    void recordEvaluation(MatchEvaluationSubmittedEvent event);
}
