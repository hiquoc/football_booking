package com.project.booking.controller;

import com.project.booking.dto.request.CancelBookingRequest;
import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.request.UpdateReservationRequest;
import com.project.booking.dto.request.UpsertMatchResultRequest;
import com.project.booking.dto.response.AvailabilityResponse;
import com.project.booking.dto.response.BookingConfigResponse;
import com.project.booking.dto.response.BookingResponse;
import com.project.booking.service.BookingConfigService;
import com.project.booking.service.BookingService;
import com.project.common.dto.ApiResponse;
import com.project.common.dto.PageResponse;
import com.project.common.enums.ApiStatusCode;
import com.project.common.security.CurrentUser;
import com.project.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import com.project.booking.repository.BookingRepository;
import com.project.common.enums.BookingStatus;
import com.project.common.enums.SportType;
import com.project.common.enums.SubFieldType;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management and availability")
public class BookingController {

    private final BookingService bookingService;
    private final BookingConfigService bookingConfigService;
    private final com.project.booking.service.MatchResultService matchResultService;
    private final BookingRepository bookingRepository;

    @Operation(summary = "Check booking conflicts", description = "Internal endpoint used before creating or updating field closures.")
    @GetMapping("/internal/conflicts")
    public ApiResponse<Boolean> hasBookingConflicts(
            @RequestParam Collection<UUID> subFieldIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        boolean conflicts = bookingRepository.existsBySubFieldIdInAndBookingDateBetweenAndStatusIn(
                subFieldIds, startDate, endDate,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
        return ApiResponse.success(conflicts);
    }

    @Operation(summary = "Check completed booking at field", description = "Internal endpoint used by field-service to verify a user has completed a booking at a field before submitting a review.")
    @GetMapping("/internal/completed-at-field")
    public ApiResponse<Boolean> hasCompletedBookingAtField(
            @RequestParam UUID userId,
            @RequestParam UUID fieldId) {
        boolean completed = bookingRepository.existsCompletedBookingAtField(userId, fieldId, BookingStatus.COMPLETED);
        return ApiResponse.success(completed);
    }

    @Operation(summary = "Get booking configuration", description = "Returns public booking configuration used for booking fee estimates.")
    @GetMapping("/config")
    public ApiResponse<BookingConfigResponse> getBookingConfig() {
        return ApiResponse.success(bookingConfigService.getCurrent());
    }

    @Operation(
        summary = "Create booking",
        description = "Creates a new normal booking for an authenticated client, employee, or owner booking another owner's field. Owners cannot create normal bookings for fields they own.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                schema = @Schema(implementation = CreateBookingRequest.class),
                examples = @ExampleObject(value = """
                    {
                      "subFieldId": "123e4567-e89b-12d3-a456-426614174001",
                      "bookingDate": "2025-06-20",
                      "startTime": "08:00:00",
                      "durationMinutes": 120,
                      "note": "Please prepare extra balls"
                    }
                    """)
            )
        )
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Booking created",
            content = @Content(
                schema = @Schema(implementation = BookingResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "message": "Booking created successfully",
                      "data": """ + BOOKING_EXAMPLE + """
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Time slot already booked or invalid input",
            content = @Content(
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "message": "Time slot is already booked",
                      "data": null
                    }
                    """)
            )
        )
    })
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE','OWNER')")
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = bookingService.createBooking(user.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ApiStatusCode.BOOKING_CREATED, "Booking created successfully", response));
    }

    @Operation(
        summary = "Create reservation",
        description = "Creates an owner reservation on an owned field. Reservations block availability like confirmed bookings, always cost 0 VND, and do not create payment records."
    )
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/owner/reservations")
    public ResponseEntity<ApiResponse<BookingResponse>> createReservation(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = bookingService.createReservation(user.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ApiStatusCode.RESERVATION_CREATED, "Reservation created successfully", response));
    }

    @Operation(summary = "Update reservation", description = "Updates an owner reservation and keeps its total price at 0 VND.")
    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/owner/reservations/{reservationId}")
    public ResponseEntity<ApiResponse<BookingResponse>> updateReservation(
            @PathVariable UUID reservationId,
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody UpdateReservationRequest request) {
        request.setReservationId(reservationId);
        BookingResponse response = bookingService.updateReservation(user.id(), request);
        return ResponseEntity.ok(ApiResponse.success(ApiStatusCode.RESERVATION_UPDATED, "Reservation updated successfully", response));
    }

    @Operation(summary = "Cancel reservation", description = "Cancels an owner reservation without payment or refund side effects.")
    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/owner/reservations/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelReservation(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody CancelBookingRequest request) {
        BookingResponse response = bookingService.cancelReservation(user.id(), request);
        return ResponseEntity.ok(ApiResponse.success(ApiStatusCode.RESERVATION_CANCELLED, "Reservation cancelled successfully", response));
    }

    @Operation(
        summary = "Cancel booking",
        description = "Cancels a booking owned by the authenticated client",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                schema = @Schema(implementation = CancelBookingRequest.class),
                examples = @ExampleObject(value = """
                    {
                      "bookingId": "123e4567-e89b-12d3-a456-426614174000",
                      "reason": "Change of plans"
                    }
                    """)
            )
        )
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Booking cancelled",
            content = @Content(
                schema = @Schema(implementation = BookingResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "message": "Booking cancelled successfully",
                      "data": """ + BOOKING_EXAMPLE + """
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Booking not found",
            content = @Content(
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "message": "Booking not found",
                      "data": null
                    }
                    """)
            )
        )
    })
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE','OWNER')")
    @PatchMapping("/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody CancelBookingRequest request) {

        BookingResponse response = bookingService.cancelBooking(user.id(), request);
        return ResponseEntity.ok(ApiResponse.success(ApiStatusCode.BOOKING_CANCELLED, "Booking cancelled successfully", response));
    }

    @Operation(summary = "Pay pending booking from wallet", description = "Deducts the booking fee from the authenticated user's wallet and confirms the pending booking.")
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE','OWNER')")
    @PostMapping("/{bookingId}/pay")
    public ResponseEntity<ApiResponse<BookingResponse>> payPendingBooking(
            @PathVariable UUID bookingId,
            @CurrentUser UserPrincipal user) {
        BookingResponse response = bookingService.payPendingBooking(user.id(), bookingId);
        return ResponseEntity.ok(ApiResponse.success(ApiStatusCode.BOOKING_PAID, "Booking paid successfully", response));
    }

    @Operation(
        summary = "Owner cancel booking",
        description = "Cancels a booking owned by the authenticated field owner",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                schema = @Schema(implementation = CancelBookingRequest.class),
                examples = @ExampleObject(value = """
                    {
                      "bookingId": "123e4567-e89b-12d3-a456-426614174000",
                      "reason": "Field unavailable"
                    }
                    """)
            )
        )
    )
    @PreAuthorize("hasAnyRole('OWNER','EMPLOYEE')")
    @PatchMapping("/owner/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBookingByOwner(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody CancelBookingRequest request) {

        BookingResponse response = bookingService.cancelBookingByManager(user.id(), user.role(), request);
        return ResponseEntity.ok(ApiResponse.success(ApiStatusCode.BOOKING_CANCELLED, "Booking cancelled successfully", response));
    }

    @Operation(summary = "Get my bookings", description = "Returns paginated booking history for the authenticated client. Supports bookingDate, status, page, size, and sort query parameters.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Page of bookings",
            content = @Content(
                schema = @Schema(implementation = BookingResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "message": null,
                      "data": {
                        "content": [""" + BOOKING_EXAMPLE + """
                        ],
                        "page": 0,
                        "size": 20,
                        "totalElements": 1,
                        "totalPages": 1,
                        "first": true,
                        "last": true,
                        "empty": false
                      }
                    }
                    """)
            )
        )
    })
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE','OWNER')")
    @PageableAsQueryParam
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getMyBookings(
            @Parameter(hidden = true) @CurrentUser UserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate,
            @RequestParam(required = false) BookingStatus status,
            @Parameter(hidden = true)
            Pageable pageable) {

        PageResponse<BookingResponse> bookings = bookingService.getMyBookings(user.id(), bookingDate, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @Operation(summary = "Get owner bookings", description = "Returns paginated bookings across the authenticated owner's fields. Supports bookingDate, fieldId, fieldType, subFieldType, status, page, size, and sort query parameters.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Page of bookings for the owner",
            content = @Content(
                schema = @Schema(implementation = BookingResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "message": null,
                      "data": {
                        "content": [""" + BOOKING_EXAMPLE + """
                        ],
                        "page": 0,
                        "size": 20,
                        "totalElements": 1,
                        "totalPages": 1,
                        "first": true,
                        "last": true,
                        "empty": false
                      }
                    }
                    """)
            )
        )
    })
    @PreAuthorize("hasAnyRole('OWNER','EMPLOYEE')")
    @PageableAsQueryParam
    @GetMapping("/owner")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getOwnerBookings(
            @Parameter(hidden = true) @CurrentUser UserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate,
            @RequestParam(required = false) UUID fieldId,
            @RequestParam(required = false) SportType fieldType,
            @RequestParam(required = false) SubFieldType subFieldType,
            @RequestParam(required = false) BookingStatus status,
            @Parameter(hidden = true)
            Pageable pageable) {

        PageResponse<BookingResponse> bookings = bookingService.getManagerBookings(user.id(), user.role(), bookingDate, fieldId, fieldType, subFieldType, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @Operation(summary = "Get owner reservations", description = "Returns paginated reservations across managed sub-fields. Supports bookingDate, subFieldId, status, page, size, and sort query parameters.")
    @PreAuthorize("hasAnyRole('OWNER','EMPLOYEE')")
    @PageableAsQueryParam
    @GetMapping("/owner/reservations")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getOwnerReservations(
            @Parameter(hidden = true) @CurrentUser UserPrincipal user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate,
            @RequestParam(required = false) UUID subFieldId,
            @RequestParam(required = false) BookingStatus status,
            @Parameter(hidden = true)
            Pageable pageable) {

        PageResponse<BookingResponse> reservations = bookingService.getManagerReservations(user.id(), user.role(), bookingDate, subFieldId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(reservations));
    }

    @PreAuthorize("hasAnyRole('OWNER','EMPLOYEE')")
    @PutMapping("/owner/{bookingId}/match-result")
    public ResponseEntity<ApiResponse<BookingResponse>> upsertMatchResult(
            @PathVariable UUID bookingId,
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody UpsertMatchResultRequest request) {
        BookingResponse response = matchResultService.upsert(user.id(), user.role(), bookingId, request);
        return ResponseEntity.ok(ApiResponse.success(ApiStatusCode.MATCH_RESULT_SAVED, "Match result saved successfully", response));
    }

    @Operation(summary = "Get booking by ID", description = "Returns the detail of a single booking")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Booking detail",
            content = @Content(
                schema = @Schema(implementation = BookingResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "message": null,
                      "data": """ + BOOKING_EXAMPLE + """
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Booking not found",
            content = @Content(
                examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "message": "Booking not found",
                      "data": null
                    }
                    """)
            )
        )
    })
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE','OWNER')")
    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable UUID bookingId,
            @CurrentUser UserPrincipal user) {

        BookingResponse response = bookingService.getBookingById(bookingId, user.id());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
        summary = "Get availability",
        description = "Returns operating hours and unavailable booking ranges for a given sub-field and date"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Availability information",
            content = @Content(
                schema = @Schema(implementation = AvailabilityResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "message": null,
                      "data": {
                        "openTime": "06:00:00",
                        "closeTime": "23:00:00",
                        "unavailableSlots": [
                          { "startTime": "09:00:00", "endTime": "10:30:00" }
                        ]
                      }
                    }
                    """)
            )
        )
    })
    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> getAvailability(
            @RequestParam UUID subFieldId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        AvailabilityResponse availability = bookingService.getAvailability(subFieldId, date);
        return ResponseEntity.ok(ApiResponse.success(availability));
    }

    private static final String BOOKING_EXAMPLE = """
            {
              "id": "123e4567-e89b-12d3-a456-426614174000",
              "bookingCode": "BK-20250613-0001",
              "clientId": "550e8400-e29b-41d4-a716-446655440000",
              "subFieldId": "123e4567-e89b-12d3-a456-426614174001",
              "subFieldName": "San A",
              "fieldName": "San Bong Quan 1",
              "ownerId": "550e8400-e29b-41d4-a716-446655440001",
              "bookingDate": "2025-06-20",
              "startTime": "08:00:00",
              "endTime": "10:00:00",
              "durationMinutes": 120,
              "pricePerHour": 150000,
              "subFieldPrice": 300000,
              "bookingPrice": 1000,
              "platformBookingFee": 1000,
              "status": "CONFIRMED",
              "note": "Please prepare extra balls",
              "cancellationReason": null,
              "cancelledAt": null,
              "cancelledBy": null,
              "createdAt": "2025-06-13T09:00:00",
              "updatedAt": "2025-06-13T09:00:00"
            }
            """;

}
