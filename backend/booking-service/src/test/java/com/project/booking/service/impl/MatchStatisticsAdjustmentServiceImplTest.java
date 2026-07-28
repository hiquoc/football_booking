package com.project.booking.service.impl;

import com.project.booking.community.entity.CommunityApplication;
import com.project.booking.community.entity.CommunityPost;
import com.project.booking.community.enums.CommunityApplicationStatus;
import com.project.booking.community.enums.CommunityPostType;
import com.project.booking.community.repository.CommunityPostRepository;
import com.project.booking.entity.Booking;
import com.project.booking.enums.WinningTeam;
import com.project.booking.kafka.MatchResultEventPublisher;
import com.project.common.events.notification.PlayerMatchStatisticsAdjustedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchStatisticsAdjustmentServiceImplTest {
    private static final UUID BOOKING_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOOKER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OPPONENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID APPLICATION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private final CommunityPostRepository communityPostRepository = mock(CommunityPostRepository.class);
    private final MatchResultEventPublisher eventPublisher = mock(MatchResultEventPublisher.class);
    private final MatchStatisticsAdjustmentServiceImpl service =
            new MatchStatisticsAdjustmentServiceImpl(communityPostRepository, eventPublisher);

    @Test
    void normalBookingUpdatesOnlyBookerForBookerWin() {
        when(communityPostRepository.findFirstByBookingIdAndPostType(BOOKING_ID, CommunityPostType.LOOKING_OPPONENT))
                .thenReturn(Optional.empty());

        service.adjustForResultChange(booking(), null, WinningTeam.BOOKER_WIN);

        List<PlayerMatchStatisticsAdjustedEvent> events = publishedEvents();
        assertThat(events).hasSize(1);
        assertDelta(events.get(0), BOOKER_ID, 1, 1, 0, 0);
    }

    @Test
    void communityBookingUpdatesBookerAndOpponentForBookerLoss() {
        when(communityPostRepository.findFirstByBookingIdAndPostType(BOOKING_ID, CommunityPostType.LOOKING_OPPONENT))
                .thenReturn(Optional.of(communityPost()));

        service.adjustForResultChange(booking(), null, WinningTeam.BOOKER_LOSS);

        List<PlayerMatchStatisticsAdjustedEvent> events = publishedEvents();
        assertThat(events).hasSize(2);
        assertDelta(events.get(0), BOOKER_ID, 1, 0, 1, 0);
        assertDelta(events.get(1), OPPONENT_ID, 1, 1, 0, 0);
    }

    @Test
    void communityBookingUpdatesBothPlayersForDraw() {
        when(communityPostRepository.findFirstByBookingIdAndPostType(BOOKING_ID, CommunityPostType.LOOKING_OPPONENT))
                .thenReturn(Optional.of(communityPost()));

        service.adjustForResultChange(booking(), null, WinningTeam.DRAW);

        List<PlayerMatchStatisticsAdjustedEvent> events = publishedEvents();
        assertThat(events).hasSize(2);
        assertDelta(events.get(0), BOOKER_ID, 1, 0, 0, 1);
        assertDelta(events.get(1), OPPONENT_ID, 1, 0, 0, 1);
    }

    @Test
    void updateExistingResultPublishesOnlyOutcomeDelta() {
        when(communityPostRepository.findFirstByBookingIdAndPostType(BOOKING_ID, CommunityPostType.LOOKING_OPPONENT))
                .thenReturn(Optional.of(communityPost()));

        service.adjustForResultChange(booking(), WinningTeam.BOOKER_WIN, WinningTeam.DRAW);

        List<PlayerMatchStatisticsAdjustedEvent> events = publishedEvents();
        assertThat(events).hasSize(2);
        assertDelta(events.get(0), BOOKER_ID, 0, -1, 0, 1);
        assertDelta(events.get(1), OPPONENT_ID, 0, 0, -1, 1);
    }

    @Test
    void duplicateSubmissionDoesNotPublishStatisticsAgain() {
        when(communityPostRepository.findFirstByBookingIdAndPostType(BOOKING_ID, CommunityPostType.LOOKING_OPPONENT))
                .thenReturn(Optional.of(communityPost()));

        service.adjustForResultChange(booking(), WinningTeam.BOOKER_WIN, WinningTeam.BOOKER_WIN);

        verify(eventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    private Booking booking() {
        return Booking.builder()
                .id(BOOKING_ID)
                .clientId(BOOKER_ID)
                .build();
    }

    private CommunityPost communityPost() {
        CommunityApplication application = CommunityApplication.builder()
                .id(APPLICATION_ID)
                .applicantId(OPPONENT_ID)
                .status(CommunityApplicationStatus.ACCEPTED)
                .build();
        return CommunityPost.builder()
                .bookingId(BOOKING_ID)
                .ownerId(BOOKER_ID)
                .postType(CommunityPostType.LOOKING_OPPONENT)
                .matchedApplicationId(APPLICATION_ID)
                .applications(List.of(application))
                .build();
    }

    private List<PlayerMatchStatisticsAdjustedEvent> publishedEvents() {
        ArgumentCaptor<PlayerMatchStatisticsAdjustedEvent> captor =
                ArgumentCaptor.forClass(PlayerMatchStatisticsAdjustedEvent.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publish(captor.capture());
        return captor.getAllValues();
    }

    private void assertDelta(
            PlayerMatchStatisticsAdjustedEvent event,
            UUID userId,
            int totalMatches,
            int wins,
            int losses,
            int draws) {
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.totalMatchesDelta()).isEqualTo(totalMatches);
        assertThat(event.winsDelta()).isEqualTo(wins);
        assertThat(event.lossesDelta()).isEqualTo(losses);
        assertThat(event.drawsDelta()).isEqualTo(draws);
    }
}
