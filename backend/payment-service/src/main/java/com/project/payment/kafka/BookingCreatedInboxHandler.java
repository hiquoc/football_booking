package com.project.payment.kafka;
import com.project.common.events.notification.*;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.handler.InboxEventHandler;
import com.project.common.inbox.service.InboxService;
import com.project.payment.entity.BookingPaymentProjection;
import com.project.payment.repository.BookingPaymentProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component @RequiredArgsConstructor
public class BookingCreatedInboxHandler implements InboxEventHandler {
    private final InboxService inboxService; private final BookingPaymentProjectionRepository repository;
    @Override public boolean supports(String topic) { return NotificationEventTopics.BOOKING_CREATED.equals(topic); }
    @Override @Transactional public void handle(InboxEvent inboxEvent) {
        BookingCreatedEvent event = inboxService.payload(inboxEvent, BookingCreatedEvent.class);
        if ("RESERVATION".equals(event.bookingType())) {
            repository.deleteById(event.bookingId());
            return;
        }
        BookingPaymentProjection projection = repository.findById(event.bookingId())
                .orElseGet(() -> BookingPaymentProjection.builder().bookingId(event.bookingId()).build());
        projection.setBookingCode(event.bookingCode()); projection.setUserId(event.userId());
        projection.setUserEmail(event.userEmail());
        projection.setSubFieldPrice(event.subFieldPrice());
        projection.setBookingPrice(event.bookingPrice() == null ? 0L : event.bookingPrice());
        projection.setPlatformBookingFee(event.platformBookingFee() == null ? projection.getBookingPrice() : event.platformBookingFee());
        repository.save(projection);
    }
}
