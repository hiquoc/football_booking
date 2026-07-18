package com.project.booking.service.impl;

import com.project.booking.community.entity.CommunityApplication;
import com.project.booking.community.enums.CommunityApplicationStatus;
import com.project.booking.community.enums.CommunityPostType;
import com.project.booking.community.repository.CommunityPostRepository;
import com.project.booking.dto.request.UpsertMatchResultRequest;
import com.project.booking.dto.response.BookingResponse;
import com.project.booking.entity.Booking;
import com.project.booking.entity.MatchResult;
import com.project.booking.enums.WinningTeam;
import com.project.booking.exception.BookingNotFoundException;
import com.project.booking.kafka.MatchResultEventPublisher;
import com.project.booking.mapper.BookingMapper;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.MatchResultRepository;
import com.project.booking.service.MatchResultService;
import com.project.common.enums.BookingStatus;
import com.project.common.events.notification.PlayerMatchStatisticsAdjustedEvent;
import com.project.common.exception.BadRequestException;
import com.project.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchResultServiceImpl implements MatchResultService {
    private final BookingRepository bookingRepository;
    private final MatchResultRepository matchResultRepository;
    private final CommunityPostRepository communityPostRepository;
    private final MatchResultEventPublisher eventPublisher;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public BookingResponse upsert(UUID ownerId, UUID bookingId, UpsertMatchResultRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        validateOwnerCanSubmit(ownerId, booking);
        validateSplit(request);

        Optional<MatchResult> previous = matchResultRepository.findByBookingId(bookingId);
        WinningTeam previousWinner = previous.map(MatchResult::getWinningTeam).orElse(null);

        MatchResult result = previous.orElseGet(() -> MatchResult.builder()
                .bookingId(bookingId)
                .build());
        result.setWinningTeam(request.getWinningTeam());
        result.setTeamAPercentage(request.getTeamAPercentage());
        result.setTeamBPercentage(request.getTeamBPercentage());
        result.setTeamAAmount(calculateAmount(booking.getTotalAmount(), request.getTeamAPercentage()));
        result.setTeamBAmount(calculateAmount(booking.getTotalAmount(), request.getTeamBPercentage()));
        result.setSubmittedBy(ownerId);

        MatchResult saved = matchResultRepository.save(result);
        publishStatisticsAdjustment(bookingId, previousWinner, saved.getWinningTeam());

        BookingResponse response = bookingMapper.toResponse(booking);
        response.setMatchResult(bookingMapper.toMatchResultResponse(saved));
        return response;
    }

    private void validateOwnerCanSubmit(UUID ownerId, Booking booking) {
        if (!booking.getOwnerId().equals(ownerId)) {
            throw new UnauthorizedException("Only the field owner may submit this match result");
        }
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            throw new BadRequestException("Cancelled or expired bookings cannot have match results");
        }
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Only completed bookings can have match results");
        }
        LocalDateTime finishedAt = LocalDateTime.of(booking.getBookingDate(), booking.getEndTime());
        if (finishedAt.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Booking has not finished yet");
        }
    }

    private void validateSplit(UpsertMatchResultRequest request) {
        if (request.getTeamAPercentage() + request.getTeamBPercentage() != 100) {
            throw new BadRequestException("Team A percentage and Team B percentage must total 100");
        }
    }

    private BigDecimal calculateAmount(BigDecimal totalAmount, int percentage) {
        return totalAmount
                .multiply(BigDecimal.valueOf(percentage))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private void publishStatisticsAdjustment(UUID bookingId, WinningTeam previousWinner, WinningTeam nextWinner) {
        communityPostRepository.findFirstByBookingIdAndPostType(bookingId, CommunityPostType.LOOKING_OPPONENT)
                .ifPresent(post -> {
                    UUID teamBUserId = post.getApplications().stream()
                            .filter(application -> application.getId().equals(post.getMatchedApplicationId()))
                            .filter(application -> application.getStatus() == CommunityApplicationStatus.ACCEPTED)
                            .map(CommunityApplication::getApplicantId)
                            .findFirst()
                            .orElse(null);
                    if (teamBUserId == null) {
                        return;
                    }
                    publishDelta(post.getOwnerId(), previousWinner, nextWinner, true);
                    publishDelta(teamBUserId, previousWinner, nextWinner, false);
                });
    }

    private void publishDelta(UUID userId, WinningTeam previousWinner, WinningTeam nextWinner, boolean teamA) {
        OutcomeDelta oldDelta = delta(previousWinner, teamA);
        OutcomeDelta newDelta = delta(nextWinner, teamA);
        int totalMatchesDelta = previousWinner == null ? 1 : 0;
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

    private OutcomeDelta delta(WinningTeam winner, boolean teamA) {
        if (winner == null) {
            return new OutcomeDelta(0, 0, 0);
        }
        if (winner == WinningTeam.DRAW) {
            return new OutcomeDelta(0, 0, 1);
        }
        boolean won = (winner == WinningTeam.TEAM_A && teamA) || (winner == WinningTeam.TEAM_B && !teamA);
        return won ? new OutcomeDelta(1, 0, 0) : new OutcomeDelta(0, 1, 0);
    }

    private record OutcomeDelta(int wins, int losses, int draws) {
    }
}
