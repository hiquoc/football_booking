package com.project.booking.kafka;
import com.project.booking.cache.AvailabilityCacheService;
import com.project.booking.entity.Booking;
import com.project.booking.repository.BookingRepository;
import com.project.common.enums.BookingStatus;
import com.project.common.events.notification.*;
import com.project.common.inbox.entity.InboxEvent;
import com.project.common.inbox.service.InboxService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import static org.mockito.Mockito.*;

class PaymentInboxEventHandlerTest {
    @Test void successConfirmsPendingBookingOnce() {
        InboxService inbox = mock(InboxService.class); BookingRepository bookings = mock(BookingRepository.class);
        BookingNotificationEventPublisher notifications = mock(BookingNotificationEventPublisher.class);
        AvailabilityCacheService availabilityCacheService = mock(AvailabilityCacheService.class);
        PaymentInboxEventHandler handler = new PaymentInboxEventHandler(inbox, bookings, notifications, availabilityCacheService);
        UUID bookingId = UUID.randomUUID(); UUID paymentId = UUID.randomUUID(); UUID subFieldId = UUID.randomUUID();
        InboxEvent envelope = InboxEvent.builder().topic(NotificationEventTopics.PAYMENT_SUCCESS).build();
        PaymentSuccessEvent event = new PaymentSuccessEvent(paymentId, bookingId, "BK-1", UUID.randomUUID(), null, BigDecimal.TEN, Instant.now());
        when(inbox.payload(envelope, PaymentSuccessEvent.class)).thenReturn(event);
        when(bookings.confirmPendingBookingFromPayment(bookingId, BookingStatus.PENDING, BookingStatus.CONFIRMED)).thenReturn(1);
        Booking booking = Booking.builder()
                .id(bookingId)
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .status(BookingStatus.CONFIRMED)
                .build();
        when(bookings.findById(bookingId)).thenReturn(Optional.of(booking));
        handler.handle(envelope);
        verify(availabilityCacheService).evict(booking.getSubFieldId(), booking.getBookingDate());
        verify(notifications).publishBookingConfirmed(booking, null);
    }
}
