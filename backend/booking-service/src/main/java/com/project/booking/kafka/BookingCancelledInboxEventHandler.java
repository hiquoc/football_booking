package com.project.booking.kafka;

import com.project.booking.community.service.CommunityPostMaintenanceService;
import com.project.common.events.notification.BookingCancelledEvent;
import com.project.common.events.notification.NotificationEventTopics;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.handler.InboxEventHandler;
import com.project.common.inbox.service.InboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCancelledInboxEventHandler implements InboxEventHandler {
    private final InboxService inboxService;
    private final CommunityPostMaintenanceService communityPostMaintenanceService;

    @Override
    public boolean supports(String topic) {
        return NotificationEventTopics.BOOKING_CANCELLED.equals(topic);
    }

    @Override
    @Transactional
    public void handle(InboxEvent event) {
        BookingCancelledEvent payload = inboxService.payload(event, BookingCancelledEvent.class);
        communityPostMaintenanceService.cancelOpenPostForBooking(payload.bookingId());
        log.info("Cancelled open community posts after booking cancellation event: bookingId={}", payload.bookingId());
    }
}
