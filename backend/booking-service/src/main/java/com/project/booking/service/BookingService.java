package com.project.booking.service;

import com.project.booking.dto.request.CancelBookingRequest;
import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.response.AvailabilityResponse;
import com.project.booking.dto.response.BookingResponse;
import com.project.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface BookingService {

    /**
     * Creates a new booking after performing all validation checks.
     *
     * @param userId the authenticated user's ID (from gateway header)
     * @param request  the booking creation request
     * @return the created booking response
     */
    BookingResponse createBooking(UUID userId, CreateBookingRequest request);

    /**
     * Cancels an existing booking owned by the requesting client.
     *
     * @param userId the authenticated user's ID
     * @param request  contains booking ID and optional cancellation reason
     * @return the updated booking response
     */
    BookingResponse cancelBooking(UUID userId, CancelBookingRequest request);

    /**
     * Cancels an existing booking owned by the requesting field owner.
     *
     * @param ownerId the authenticated owner's ID
     * @param request contains booking ID and optional cancellation reason
     * @return the updated booking response
     */
    BookingResponse cancelBookingByOwner(UUID ownerId, CancelBookingRequest request);

    /**
     * Mock payment confirmation endpoint. Confirms a pending booking for now.
     *
     * @param userId    the authenticated client's ID
     * @param bookingId the booking to confirm
     * @return the updated booking response
     */
    BookingResponse confirmMockPayment(UUID userId, UUID bookingId);

    /**
     * Expires pending bookings older than the configured payment timeout.
     *
     * @return number of bookings expired
     */
    int expirePendingBookings();

    /**
     * Completes confirmed bookings whose booked time has ended.
     *
     * @return number of bookings completed
     */
    int completeFinishedBookings();

    /**
     * Returns the booking history for a client.
     *
     * @param userId the authenticated user's ID
     * @return page of bookings for that client
     */
    PageResponse<BookingResponse> getMyBookings(UUID userId, Pageable pageable);

    /**
     * Returns all bookings for a field owner across their sub-fields.
     *
     * @param ownerId the authenticated owner's ID
     * @return page of bookings for that owner
     */
    PageResponse<BookingResponse> getOwnerBookings(UUID ownerId, Pageable pageable);

    /**
     * Returns a booking by ID.
     *
     * @param bookingId the booking ID
     * @param userId  the requesting user (for ownership check)
     * @return the booking response
     */
    BookingResponse getBookingById(UUID bookingId, UUID userId);

    /**
     * Returns operating hours and unavailable booking ranges for a sub-field on a given date.
     *
     * @param subFieldId  the sub-field to check
     * @param date        the date to query
     * @return availability response with booked time ranges
     */
    AvailabilityResponse getAvailability(UUID subFieldId, LocalDate date);
}
