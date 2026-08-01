package com.project.field.controller;

import com.project.common.dto.ApiResponse;
import com.project.common.dto.PageResponse;
import com.project.common.security.CurrentUser;
import com.project.common.security.UserPrincipal;
import com.project.field.dto.ReviewDto;
import com.project.field.dto.ReviewRequest;
import com.project.field.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Submit and view user reviews for fields. Automatically recalculates the field's rating average.")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(
            summary = "Submit a review",
            description = "Posts a rating and optional comment for a field. The field's average rating and total review count are updated automatically.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ReviewRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "fieldId": "123e4567-e89b-12d3-a456-426614174001",
                                      "rating": 5,
                                      "comment": "Great facility, well-maintained pitch!"
                                    }
                                    """)
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Review submitted",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Review submitted successfully",
                                      "data": {
                                        "id": "123e4567-e89b-12d3-a456-426614174000",
                                        "fieldId": "123e4567-e89b-12d3-a456-426614174001",
                                        "userId": "550e8400-e29b-41d4-a716-446655440000",
                                        "fullName": "Nguyen Van A",
                                        "rating": 5,
                                        "comment": "Great facility, well-maintained pitch!"
                                      }
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Field not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('CLIENT','EMPLOYEE')")
    @PostMapping
    public ApiResponse<ReviewDto> create(
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.success("Review submitted successfully", reviewService.create(user, request));
    }

    @Operation(summary = "Get reviews for a field", description = "Returns paginated reviews submitted for the specified field, ordered by newest first. Reviewer contact data is not exposed; fullName is either the user's name or a masked fallback.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reviews returned")
    @GetMapping("/field/{fieldId}")
    public ApiResponse<PageResponse<ReviewDto>> getByFieldId(
            @PathVariable UUID fieldId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 20)));
        return ApiResponse.success(reviewService.getByFieldId(fieldId, pageable));
    }
}
