package com.project.booking.kafka;

import com.project.booking.entity.Booking;
import com.project.booking.entity.UserProjection;
import com.project.booking.repository.BookingRepository;
import com.project.booking.repository.UserProjectionRepository;
import com.project.booking.service.PendingBookingReservationService;
import com.project.common.enums.BookingStatus;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.events.notification.UserBalanceUpdatedEvent;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.handler.InboxEventHandler;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserBalanceUpdatedInboxEventHandler implements InboxEventHandler {
    private static final String REASON = "BOOKING_ACCOUNT_BALANCE_PAYMENT";

    private final InboxService inboxService;
    private final BookingRepository bookingRepository;
    private final UserProjectionRepository userProjectionRepository;
    private final PendingBookingReservationService pendingBookingReservationService;
    private final BookingBalanceEventPublisher balanceEventPublisher;

    @Override
    public boolean supports(String topic) {
        return NotificationEventTopics.USER_BALANCE_UPDATED.equals(topic);
    }

    @Override
    @Transactional
    public void handle(InboxEvent event) {
        UserBalanceUpdatedEvent balance = inboxService.payload(event, UserBalanceUpdatedEvent.class);
        UserProjection projection = userProjectionRepository.findById(balance.userId())
                .orElseGet(() -> UserProjection.builder().userId(balance.userId()).build());
        projection.setBalance(balance.balance());
        userProjectionRepository.save(projection);
        if (!"WALLET_TOP_UP".equals(balance.reason())) {
            return;
        }
        pendingBookingReservationService.find(balance.userId())
                .flatMap(bookingRepository::findById)
                .filter(booking -> booking.getStatus() == BookingStatus.PENDING)
                .filter(booking -> booking.getSourceRecurringBookingId() == null)
                .filter(booking -> balance.balance() >= payableAmount(booking))
                .ifPresent(booking -> balanceEventPublisher.publishDeductionRequested(booking, REASON));
    }

    private long payableAmount(Booking booking) {
        return booking.getBookingPrice() == null || booking.getBookingPrice() == 0L
                ? (booking.getPlatformBookingFee() == null ? 0L : booking.getPlatformBookingFee())
                : booking.getBookingPrice();
    }
}
