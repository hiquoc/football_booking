package com.project.booking.kafka;
import com.project.booking.cache.AvailabilityCacheService;
import com.project.booking.entity.Booking;
import com.project.booking.repository.BookingRepository;
import com.project.booking.service.PendingBookingReservationService;
import com.project.common.enums.BookingPaymentStatus;
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
        BookingBalanceEventPublisher balanceEventPublisher = mock(BookingBalanceEventPublisher.class);
        AvailabilityCacheService availabilityCacheService = mock(AvailabilityCacheService.class);
        PendingBookingReservationService pendingBookingReservationService = mock(PendingBookingReservationService.class);
        PaymentInboxEventHandler handler = new PaymentInboxEventHandler(
                inbox, bookings, notifications, balanceEventPublisher, availabilityCacheService, pendingBookingReservationService);
        UUID bookingId = UUID.randomUUID(); UUID paymentId = UUID.randomUUID(); UUID subFieldId = UUID.randomUUID();
        InboxEvent envelope = InboxEvent.builder().topic(NotificationEventTopics.PAYMENT_SUCCESS).build();
        PaymentSuccessEvent event = new PaymentSuccessEvent(paymentId, bookingId, "BK-1", UUID.randomUUID(), null, BigDecimal.TEN, Instant.now());
        when(inbox.payload(envelope, PaymentSuccessEvent.class)).thenReturn(event);
        when(bookings.confirmPendingBookingFromPayment(bookingId, BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingPaymentStatus.PAID)).thenReturn(1);
        Booking booking = Booking.builder()
                .id(bookingId)
                .subFieldId(subFieldId)
                .bookingDate(LocalDate.now().plusDays(1))
                .status(BookingStatus.CONFIRMED)
                .build();
        when(bookings.findById(bookingId)).thenReturn(Optional.of(booking));
        handler.handle(envelope);
        verify(availabilityCacheService).evict(booking.getSubFieldId(), booking.getBookingDate());
        verify(pendingBookingReservationService).release(booking);
        verify(notifications).publishBookingConfirmed(booking, null);
    }

    @Test void successRefundsCancelledBookingWhenFallbackCannotConfirm() {
        InboxService inbox = mock(InboxService.class); BookingRepository bookings = mock(BookingRepository.class);
        BookingNotificationEventPublisher notifications = mock(BookingNotificationEventPublisher.class);
        BookingBalanceEventPublisher balanceEventPublisher = mock(BookingBalanceEventPublisher.class);
        AvailabilityCacheService availabilityCacheService = mock(AvailabilityCacheService.class);
        PendingBookingReservationService pendingBookingReservationService = mock(PendingBookingReservationService.class);
        PaymentInboxEventHandler handler = new PaymentInboxEventHandler(
                inbox, bookings, notifications, balanceEventPublisher, availabilityCacheService, pendingBookingReservationService);
        UUID bookingId = UUID.randomUUID(); UUID paymentId = UUID.randomUUID();
        InboxEvent envelope = InboxEvent.builder().topic(NotificationEventTopics.PAYMENT_SUCCESS).build();
        PaymentSuccessEvent event = new PaymentSuccessEvent(paymentId, bookingId, "BK-1", UUID.randomUUID(), null, new BigDecimal("12000"), Instant.now());
        Booking booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CANCELLED)
                .paymentStatus(BookingPaymentStatus.FAILED)
                .build();
        when(inbox.payload(envelope, PaymentSuccessEvent.class)).thenReturn(event);
        when(bookings.confirmPendingBookingFromPayment(bookingId, BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingPaymentStatus.PAID)).thenReturn(0);
        when(bookings.findById(bookingId)).thenReturn(Optional.of(booking));

        handler.handle(envelope);

        verify(balanceEventPublisher).publishRefundRequested(booking, 12000L, "BOOKING_PAYMENT_REFUND");
        verify(bookings).markPaymentRefunded(bookingId, BookingPaymentStatus.REFUNDED);
        verify(notifications, never()).publishBookingConfirmed(any(), any());
    }
}
