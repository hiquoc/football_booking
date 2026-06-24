package com.project.booking.controller;

import com.project.booking.dto.request.CancelBookingRequest;
import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.response.AvailabilityResponse;
import com.project.booking.dto.response.BookingResponse;
import com.project.booking.service.BookingService;
import com.project.common.dto.ApiResponse;
import com.project.common.security.CurrentUser;
import com.project.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management and availability")
public class BookingController {

    private final BookingService bookingService;

    @Operation(
        summary = "Create booking",
        description = "Creates a new booking for the authenticated client",
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
    @PreAuthorize("hasRole('CLIENT')")
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody CreateBookingRequest request) {

        BookingResponse response = bookingService.createBooking(user.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", response));
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
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody CancelBookingRequest request) {

        BookingResponse response = bookingService.cancelBooking(user.id(), request);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", response));
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
    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/owner/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBookingByOwner(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody CancelBookingRequest request) {

        BookingResponse response = bookingService.cancelBookingByOwner(user.id(), request);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", response));
    }

    @Operation(
        summary = "Mock payment confirmation",
        description = "Mock payment endpoint that marks a pending booking as confirmed"
    )
    @PreAuthorize("hasRole('CLIENT')")
    @PatchMapping("/{bookingId}/mock-payment")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmMockPayment(
            @CurrentUser UserPrincipal user,
            @PathVariable UUID bookingId) {

        BookingResponse response = bookingService.confirmMockPayment(user.id(), bookingId);
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed successfully", response));
    }

    @Operation(summary = "Get my bookings", description = "Returns the booking history for the authenticated client")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "List of bookings",
            content = @Content(
                schema = @Schema(implementation = BookingResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "message": null,
                      "data": [""" + BOOKING_EXAMPLE + """
                      ]
                    }
                    """)
            )
        )
    })
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            @CurrentUser UserPrincipal user) {

        List<BookingResponse> bookings = bookingService.getMyBookings(user.id());
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @Operation(summary = "Get owner bookings", description = "Returns all bookings across the authenticated owner's sub-fields")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "List of bookings for the owner",
            content = @Content(
                schema = @Schema(implementation = BookingResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "message": null,
                      "data": [""" + BOOKING_EXAMPLE + """
                      ]
                    }
                    """)
            )
        )
    })
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/owner")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getOwnerBookings(
            @CurrentUser UserPrincipal user) {

        List<BookingResponse> bookings = bookingService.getOwnerBookings(user.id());
        return ResponseEntity.ok(ApiResponse.success(bookings));
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
    @PreAuthorize("hasRole('CLIENT')")
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
              "totalAmount": 300000,
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
