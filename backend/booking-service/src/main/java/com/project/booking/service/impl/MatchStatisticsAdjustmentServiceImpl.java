package com.project.booking.service.impl;

import com.project.booking.community.entity.CommunityApplication;
import com.project.booking.community.enums.CommunityApplicationStatus;
import com.project.booking.community.enums.CommunityPostType;
import com.project.booking.community.repository.CommunityPostRepository;
import com.project.booking.entity.Booking;
import com.project.booking.enums.WinningTeam;
import com.project.booking.kafka.MatchResultEventPublisher;
import com.project.booking.service.MatchStatisticsAdjustmentService;
import com.project.common.events.notification.PlayerMatchStatisticsAdjustedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchStatisticsAdjustmentServiceImpl implements MatchStatisticsAdjustmentService {
    private final CommunityPostRepository communityPostRepository;
    private final MatchResultEventPublisher eventPublisher;

    @Override
    public void adjustForResultChange(Booking booking, WinningTeam previousResult, WinningTeam nextResult) {
        WinningTeam previous = normalize(previousResult);
        WinningTeam next = normalize(nextResult);
        Map<UUID, Boolean> participants = new LinkedHashMap<>();
        participants.put(booking.getClientId(), true);

        communityPostRepository.findFirstByBookingIdAndPostType(booking.getId(), CommunityPostType.LOOKING_OPPONENT)
                .flatMap(post -> post.getApplications().stream()
                        .filter(application -> application.getId().equals(post.getMatchedApplicationId()))
                        .filter(application -> application.getStatus() == CommunityApplicationStatus.ACCEPTED)
                        .map(CommunityApplication::getApplicantId)
                        .findFirst())
                .ifPresent(opponentId -> participants.put(opponentId, false));

        participants.forEach((userId, booker) -> publishDelta(userId, previous, next, booker));
    }

    private void publishDelta(UUID userId, WinningTeam previousResult, WinningTeam nextResult, boolean booker) {
        OutcomeDelta oldDelta = delta(previousResult, booker);
        OutcomeDelta newDelta = delta(nextResult, booker);
        int totalMatchesDelta = previousResult == null ? 1 : 0;
        int winsDelta = newDelta.wins() - oldDelta.wins();
        int lossesDelta = newDelta.losses() - oldDelta.losses();
        int drawsDelta = newDelta.draws() - oldDelta.draws();
        if (totalMatchesDelta == 0 && winsDelta == 0 && lossesDelta == 0 && drawsDelta == 0) {
            return;
        }
        eventPublisher.publish(new PlayerMatchStatisticsAdjustedEvent(
                userId,
                totalMatchesDelta,
                winsDelta,
                lossesDelta,
                drawsDelta,
                Instant.now()));
    }

    private OutcomeDelta delta(WinningTeam result, boolean booker) {
        WinningTeam normalized = normalize(result);
        if (normalized == null) {
            return new OutcomeDelta(0, 0, 0);
        }
        if (normalized == WinningTeam.DRAW) {
            return new OutcomeDelta(0, 0, 1);
        }
        boolean won = (normalized == WinningTeam.BOOKER_WIN && booker)
                || (normalized == WinningTeam.BOOKER_LOSS && !booker);
        return won ? new OutcomeDelta(1, 0, 0) : new OutcomeDelta(0, 1, 0);
    }

    private WinningTeam normalize(WinningTeam result) {
        if (result == WinningTeam.TEAM_A) {
            return WinningTeam.BOOKER_WIN;
        }
        if (result == WinningTeam.TEAM_B) {
            return WinningTeam.BOOKER_LOSS;
        }
        return result;
    }

    private record OutcomeDelta(int wins, int losses, int draws) {
    }
}
