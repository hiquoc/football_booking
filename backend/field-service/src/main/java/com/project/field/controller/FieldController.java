package com.project.field.controller;

import com.project.common.security.CurrentUser;
import com.project.common.security.UserPrincipal;
import java.util.List;
import java.util.UUID;

import com.project.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.common.dto.ApiResponse;
import com.project.field.dto.FieldDto;
import com.project.field.dto.FieldDetailsDto;
import com.project.field.dto.FieldCardDto;
import java.math.BigDecimal;
import com.project.field.dto.OperatingHoursDto;
import com.project.field.dto.OperatingHoursRequest;
import com.project.field.dto.FieldRequest;
import com.project.field.dto.FieldStatusRequest;
import com.project.field.enums.FieldStatus;
import com.project.field.service.FieldScheduleService;
import com.project.field.service.FieldService;
import com.project.field.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.converters.models.PageableAsQueryParam;

@RestController
@RequestMapping("/api/v1/fields")
@RequiredArgsConstructor
@Tag(name = "Fields", description = "Manage sports fields (venues). A field is owned by a user with the OWNER role and must be approved by an admin before going live.")
public class FieldController {

    private final FieldService fieldService;
    private final FieldScheduleService fieldScheduleService;
    private final ReviewService reviewService;

    @Operation(
            summary = "Create a field",
            description = "Creates a new sports field owned by the current user. A field must include a full 7-day operatingHours schedule.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = FieldRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "name": "ABC Football Center",
                                      "description": "Premier football venue in the city",
                                      "address": "123 Nguyen Hue",
                                      "ward": "Phuong Sai Gon",
                                      "wardCode": "26743",
                                      "province": "Thanh pho Ho Chi Minh",
                                      "provinceCode": "79",
                                      "legacyWard": "Phuong Ben Nghe",
                                      "legacyWardCode": "26743",
                                      "legacyDistrict": "Quan 1",
                                      "legacyProvince": "Thanh pho Ho Chi Minh",
                                      "latitude": 10.7769,
                                      "longitude": 106.7009,
                                      "phoneNumber": "0862470050",
                                      "email": "abc@football.vn",
                                      "operatingHours": [
                                        { "dayOfWeek": "MONDAY", "openTime": "06:00:00", "closeTime": "23:00:00", "closed": false },
                                        { "dayOfWeek": "TUESDAY", "openTime": "06:00:00", "closeTime": "23:00:00", "closed": false },
                                        { "dayOfWeek": "WEDNESDAY", "openTime": "06:00:00", "closeTime": "23:00:00", "closed": false },
                                        { "dayOfWeek": "THURSDAY", "openTime": "06:00:00", "closeTime": "23:00:00", "closed": false },
                                        { "dayOfWeek": "FRIDAY", "openTime": "06:00:00", "closeTime": "23:00:00", "closed": false },
                                        { "dayOfWeek": "SATURDAY", "openTime": "08:00:00", "closeTime": "22:00:00", "closed": false },
                                        { "dayOfWeek": "SUNDAY", "closed": true }
                                      ],
                                      "active": true
                                    }
                                    """)
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Field created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Field created successfully",
                                      "data": {
                                        "id": "123e4567-e89b-12d3-a456-426614174000",
                                        "ownerId": "b1e1c606-6b76-4154-af38-7dda890395ce",
                                        "name": "ABC Football Center",
                                        "status": "PENDING",
                                        "ratingAverage": 0.0,
                                        "totalReviews": 0,
                                      "fieldTypes": []
                                      }
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping
    public ApiResponse<FieldDto> create(@Validated(FieldRequest.Create.class) @RequestBody FieldRequest request,@CurrentUser UserPrincipal currentUser) {
        return ApiResponse.success("Field created successfully", fieldService.create(currentUser.id(),request));
    }

    @Operation(summary = "Update a field",
            description = "Updates an existing field's details. Only the owner can perform this action. Supplying operatingHours replaces the full 7-day weekly schedule and synchronizes Booking Service projections.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = FieldRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "name": "ABC Football Center",
                                      "description": "Premier football venue in the city",
                                      "address": "123 Nguyen Hue",
                                      "ward": "Phuong Sai Gon",
                                      "wardCode": "26743",
                                      "province": "Thanh pho Ho Chi Minh",
                                      "provinceCode": "79",
                                      "legacyWard": "Phuong Ben Nghe",
                                      "legacyWardCode": "26743",
                                      "legacyDistrict": "Quan 1",
                                      "legacyProvince": "Thanh pho Ho Chi Minh",
                                      "latitude": 10.7769,
                                      "longitude": 106.7009,
                                      "phoneNumber": "0862470050",
                                      "email": "abc@football.vn",
                                      "operatingHours": [
                                        { "dayOfWeek": "MONDAY", "openTime": "06:00:00", "closeTime": "23:00:00", "closed": false },
                                        { "dayOfWeek": "TUESDAY", "openTime": "06:00:00", "closeTime": "23:00:00", "closed": false },
                                        { "dayOfWeek": "WEDNESDAY", "openTime": "06:00:00", "closeTime": "23:00:00", "closed": false },
                                        { "dayOfWeek": "THURSDAY", "openTime": "06:00:00", "closeTime": "23:00:00", "closed": false },
                                        { "dayOfWeek": "FRIDAY", "openTime": "06:00:00", "closeTime": "23:00:00", "closed": false },
                                        { "dayOfWeek": "SATURDAY", "openTime": "08:00:00", "closeTime": "22:00:00", "closed": false },
                                        { "dayOfWeek": "SUNDAY", "closed": true }
                                      ],
                                      "active": true
                                    }
                                    """)
                    )
            ))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Field updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Field not found", content = @Content)
    })
    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/{id}")
    public ApiResponse<FieldDto> update(@PathVariable UUID id,@CurrentUser UserPrincipal currentUser, @Valid @RequestBody FieldRequest request) {
        return ApiResponse.success("Field updated successfully", fieldService.update(id,currentUser.id(), request));
    }

    @Operation(
            summary = "Get a field by ID",
            description = "Returns the full details of a single field, including images, supported field types, and subfields. Approved fields are public. Pending or rejected fields can only be viewed by admins or by the owner of that field."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Field found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Unapproved field can only be viewed by the owner or an admin", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Field not found", content = @Content)
    })
    @GetMapping("/{id}")
    public ApiResponse<FieldDto> getById(@PathVariable UUID id, @CurrentUser UserPrincipal currentUser) {
        return ApiResponse.success(fieldService.getWithDetailsById(id,currentUser));
    }

    @Operation(
            summary = "Get complete field details",
            description = "Returns all data required by the field details page in one response. Approved fields are public. Pending or rejected fields can only be viewed by admins or by the owner of that field."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Field details returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Unapproved field can only be viewed by the owner or an admin", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Field not found", content = @Content)
    })
    @GetMapping("/{id}/details")
    public ApiResponse<FieldDetailsDto> getDetails(@PathVariable UUID id, @CurrentUser UserPrincipal currentUser) {
        FieldDto field = fieldService.getWithDetailsById(id,currentUser);
        return ApiResponse.success(FieldDetailsDto.builder()
                .field(field)
                .operatingHours(fieldScheduleService.getFieldOperatingHours(id))
                .subFields(field.getSubFields())
                .reviews(reviewService.getByFieldId(id))
                .build());
    }

    @Operation(summary = "Search compact field cards", description = "Optimized public search returning only card data and one primary image.")
    @GetMapping("/cards")
    public ApiResponse<PageResponse<FieldCardDto>> searchCards(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fieldType,
            @RequestParam(required = false) String subFieldType,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String provinceCode,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(defaultValue = "rating") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @CurrentUser UserPrincipal currentUser) {
        return ApiResponse.success(fieldService.searchCards(keyword, fieldType, subFieldType, district, provinceCode,
                latitude, longitude, radiusKm, sortBy, direction, page, size, currentUser));
    }

    @Operation(
            summary = "Get the current owner's fields",
            description = "Returns every field owned by the authenticated owner, including pending, rejected, inactive, and approved fields. Supports page, size, and sort parameters."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Owner fields returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Owner role required", content = @Content)
    })
    @PageableAsQueryParam
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/owner")
    public ApiResponse<PageResponse<FieldDto>> getOwnerFields(
            @CurrentUser UserPrincipal currentUser,
            @Parameter(hidden = true) Pageable pageable) {
        return ApiResponse.success(fieldService.getByOwnerId(currentUser.id(), pageable));
    }

    @Operation(
            summary = "Get all fields",
            description = "Returns paginated active, approved fields. Supports Spring pageable query parameters: page, size, and sort."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Fields returned successfully",
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
                                    "ownerId": "b1e1c606-6b76-4154-af38-7dda890395ce",
                                    "name": "ABC Football Center",
                                    "description": "Premier football venue in the city",
                                    "address": "123 Nguyen Hue",
                                    "ward": "Phuong Sai Gon",
                                    "wardCode": "26743",
                                    "province": "Thanh pho Ho Chi Minh",
                                    "provinceCode": "79",
                                    "legacyWard": "Phuong Ben Nghe",
                                    "legacyWardCode": "26743",
                                    "legacyDistrict": "Quan 1",
                                    "legacyProvince": "Thanh pho Ho Chi Minh",
                                    "latitude": 10.7769,
                                    "longitude": 106.7009,
                                    "phoneNumber": "0862470050",
                                    "email": "abc@football.vn",
                                    "status": "APPROVED",
                                    "active": true,
                                    "ratingAverage": 4.8,
                                    "totalReviews": 24,
                                    "images": [],
                                    "fieldTypes": []
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
    )
    @PageableAsQueryParam
    @PreAuthorize("#status == null or hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<PageResponse<FieldDto>> getAll(
            @RequestParam(required = false) FieldStatus status,
            @Parameter(hidden = true)
            Pageable pageable,
            @CurrentUser UserPrincipal currentUser) {
        return ApiResponse.success(fieldService.getAll(status, pageable, currentUser));
    }

    @Operation(summary = "Update field approval status", description = "Allows an administrator to mark a field as pending, approved, or rejected.")
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.web.bind.annotation.PatchMapping("/{id}/status")
    public ApiResponse<FieldDto> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody FieldStatusRequest request) {
        return ApiResponse.success("Field status updated successfully",
                fieldService.updateStatus(id, request.getStatus()));
    }

    @Operation(summary = "Get field operating hours", description = "Returns the configured weekly schedule for a field.")
    @GetMapping("/{id}/operating-hours")
    public ApiResponse<List<OperatingHoursDto>> getOperatingHours(@PathVariable UUID id) {
        return ApiResponse.success(fieldScheduleService.getFieldOperatingHours(id));
    }

    @Operation(summary = "Replace field operating hours", description = "Replaces the full 7-day weekly schedule for a field.")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @PutMapping("/{id}/operating-hours")
    public ApiResponse<List<OperatingHoursDto>> replaceOperatingHours(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody List<OperatingHoursRequest> request) {
        return ApiResponse.success(
                "Field operating hours updated successfully",
                fieldScheduleService.replaceFieldOperatingHours(id, currentUser.id(), currentUser.role(), request));
    }
}
