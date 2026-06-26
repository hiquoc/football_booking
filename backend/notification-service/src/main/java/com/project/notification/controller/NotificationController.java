package com.project.notification.controller;

import com.project.common.dto.ApiResponse;
import com.project.common.dto.PageResponse;
import com.project.common.security.CurrentUser;
import com.project.common.security.UserPrincipal;
import com.project.notification.dto.NotificationResponse;
import com.project.notification.dto.NotificationSummaryResponse;
import com.project.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(
        name = "Notifications",
        description = "Read and manage authenticated-user notifications. Notifications are created from Kafka domain events and delivered through configured channels."
)
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Get notifications",
            description = "Returns paginated notifications belonging to the authenticated user. Supports Spring pageable query parameters: page, size, and sort."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Notifications returned",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Operation completed successfully",
                                      "data": {
                                        "content": [
                                          {
                                            "id": "123e4567-e89b-12d3-a456-426614174000",
                                            "userId": "550e8400-e29b-41d4-a716-446655440000",
                                            "code": "BOOKING_CONFIRMED",
                                            "title": "Booking confirmed",
                                            "payload": {
                                              "bookingId": "123e4567-e89b-12d3-a456-426614174111",
                                              "fieldId": "123e4567-e89b-12d3-a456-426614174222",
                                              "fieldName": "Stadium A"
                                            },
                                            "isRead": false,
                                            "createdAt": "2026-06-25T14:30:00",
                                            "readAt": null
                                          }
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid gateway user headers", content = @Content)
    })
    @PageableAsQueryParam
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> getNotifications(
            @Parameter(hidden = true) @CurrentUser UserPrincipal user,
            @Parameter(hidden = true) Pageable pageable) {
        return ApiResponse.success(notificationService.getNotifications(user.id(), pageable));
    }

    @Operation(
            summary = "Get unread notifications",
            description = "Returns unread notifications belonging to the authenticated user, ordered by newest first."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Unread notifications returned",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Operation completed successfully",
                                      "data": [
                                        {
                                          "id": "123e4567-e89b-12d3-a456-426614174000",
                                          "userId": "550e8400-e29b-41d4-a716-446655440000",
                                          "code": "PAYMENT_SUCCESS",
                                          "title": "Payment successful",
                                          "payload": {
                                            "paymentId": "123e4567-e89b-12d3-a456-426614174333",
                                            "bookingId": "123e4567-e89b-12d3-a456-426614174111",
                                            "amount": 150000
                                          },
                                          "isRead": false,
                                          "createdAt": "2026-06-25T14:30:00",
                                          "readAt": null
                                        }
                                      ]
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid gateway user headers", content = @Content)
    })
    @GetMapping("/unread")
    public ApiResponse<List<NotificationResponse>> getUnreadNotifications(
            @Parameter(hidden = true) @CurrentUser UserPrincipal user) {
        return ApiResponse.success(notificationService.getUnreadNotifications(user.id()));
    }

    @Operation(
            summary = "Mark notification as read",
            description = "Marks one notification as read. The notification must belong to the authenticated user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Notification marked as read",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Notification marked as read",
                                      "data": {
                                        "id": "123e4567-e89b-12d3-a456-426614174000",
                                        "userId": "550e8400-e29b-41d4-a716-446655440000",
                                        "code": "BOOKING_CANCELLED",
                                        "title": "Booking cancelled",
                                        "payload": {
                                          "bookingId": "123e4567-e89b-12d3-a456-426614174111",
                                          "reason": "Maintenance"
                                        },
                                        "isRead": true,
                                        "createdAt": "2026-06-25T14:30:00",
                                        "readAt": "2026-06-25T14:35:00"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found for authenticated user", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid gateway user headers", content = @Content)
    })
    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markAsRead(
            @Parameter(hidden = true) @CurrentUser UserPrincipal user,
            @Parameter(description = "Notification ID.", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id) {
        return ApiResponse.success("Notification marked as read", notificationService.markAsRead(user.id(), id));
    }

    @Operation(
            summary = "Mark all notifications as read",
            description = "Marks all unread notifications belonging to the authenticated user as read."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Notifications marked as read",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Notifications marked as read",
                                      "data": null
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid gateway user headers", content = @Content)
    })
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@Parameter(hidden = true) @CurrentUser UserPrincipal user) {
        notificationService.markAllAsRead(user.id());
        return ApiResponse.success("Notifications marked as read", null);
    }

    @Operation(
            summary = "Count unread notifications",
            description = "Returns the number of unread notifications belonging to the authenticated user."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Unread count returned",
                    content = @Content(
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Operation completed successfully",
                                      "data": {
                                        "count": 5
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid gateway user headers", content = @Content)
    })
    @GetMapping("/unread-count")
    public ApiResponse<NotificationSummaryResponse> countUnread(
            @Parameter(hidden = true) @CurrentUser UserPrincipal user) {
        return ApiResponse.success(NotificationSummaryResponse.builder()
                .count(notificationService.countUnread(user.id()))
                .build());
    }
}
