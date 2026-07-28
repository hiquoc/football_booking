package com.project.field.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.common.dto.ApiResponse;
import com.project.common.security.CurrentUser;
import com.project.common.security.UserPrincipal;
import com.project.field.dto.FieldClosureDto;
import com.project.field.dto.FieldClosureRequest;
import com.project.field.dto.OperatingHoursDto;
import com.project.field.dto.OperatingHoursRequest;
import com.project.field.dto.SubFieldDto;
import com.project.field.dto.SubFieldFilterOptionDto;
import com.project.field.dto.SubFieldRequest;
import com.project.field.service.FieldScheduleService;
import com.project.field.service.SubFieldService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/v1/sub-fields")
@RequiredArgsConstructor
@Tag(name = "Sub-Fields", description = "Manage bookable sub-fields. A sub-field has exactly one subFieldType, whose field type must be supported by its parent field.")
public class SubFieldController {

    private final SubFieldService subFieldService;
    private final FieldScheduleService fieldScheduleService;

    @Operation(summary = "Get sub-field filter options", description = "Returns lightweight active sub-field options for booking filters.")
    @GetMapping("/filter-options")
    public ApiResponse<List<SubFieldFilterOptionDto>> getFilterOptions(
            @RequestParam(required = false) String search) {
        return ApiResponse.success(subFieldService.getFilterOptions(search));
    }

    @Operation(
            summary = "Create a sub-field under a field",
            description = "Adds a bookable sub-field. subFieldType determines its field type; for example FOOTBALL_5V5 requires the parent field to support FOOTBALL.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = SubFieldRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "name": "Pitch A",
                                      "description": "5-a-side turf pitch",
                                      "active": true,
                                      "subFieldType": "FOOTBALL_5V5",
                                      "bookingRule": {
                                        "minimumBookingDurationMinutes": 60,
                                        "maximumBookingDurationMinutes": 180,
                                        "bookingIntervalMinutes": 60
                                      },
                                      "timePriceRules": [
                                        {
                                          "startTime": "06:00:00",
                                          "endTime": "08:00:00",
                                          "hourlyPrice": 200000
                                        },
                                        {
                                          "startTime": "08:00:00",
                                          "endTime": "14:00:00",
                                          "hourlyPrice": 180000
                                        },
                                        {
                                          "startTime": "14:00:00",
                                          "endTime": "17:00:00",
                                          "hourlyPrice": 220000
                                        },
                                        {
                                          "startTime": "17:00:00",
                                          "endTime": "23:00:00",
                                          "hourlyPrice": 250000
                                        }
                                      ]
                                    }
                                    """)
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Sub-field created",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Sub-field created successfully",
                                      "data": { "id": "123e4567-e89b-12d3-a456-426614174000", "fieldId": "123e4567-e89b-12d3-a456-426614174001", "fieldType": "FOOTBALL", "name": "Pitch A", "subFieldType": "FOOTBALL_5V5" }
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Sub-field type is incompatible with the parent field", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Field not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @PostMapping("/field/{fieldId}")
    public ApiResponse<SubFieldDto> create(
            @PathVariable UUID fieldId,
            @Validated(SubFieldRequest.Create.class) @RequestBody SubFieldRequest request) {
        return ApiResponse.success("Sub-field created successfully", subFieldService.create(fieldId, request));
    }

    @Operation(summary = "Get all sub-fields for a field", description = "Returns every sub-field belonging to the given field ID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Field not found", content = @Content)
    })
    @GetMapping("/field/{fieldId}")
    public ApiResponse<List<SubFieldDto>> getByFieldId(@PathVariable UUID fieldId) {
        return ApiResponse.success(subFieldService.getByFieldId(fieldId));
    }

    @Operation(summary = "Update a sub-field", description = "Partially updates a sub-field. Omitted fields remain unchanged.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = SubFieldRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "bookingRule": {
                                        "maximumBookingDurationMinutes": 240
                                      },
                                      "timePriceRules": [
                                        {
                                          "startTime": "06:00:00",
                                          "endTime": "23:00:00",
                                          "hourlyPrice": 220000
                                        }
                                      ]
                                    }
                                    """)
                    )
            ))
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @PutMapping("/{id}")
    public ApiResponse<SubFieldDto> update(@PathVariable UUID id, @Valid @RequestBody SubFieldRequest request) {
        return ApiResponse.success("Sub-field updated successfully", subFieldService.update(id, request));
    }

    @Operation(summary = "Delete a sub-field", description = "Soft delete a sub-field.")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        subFieldService.delete(id);
        return ApiResponse.success("Sub-field deleted successfully", null);
    }

    @Operation(summary = "Get sub-field operating hour overrides", description = "Returns the custom weekly schedule for a sub-field. Empty means the sub-field uses its parent field schedule.")
    @GetMapping("/{id}/operating-hours")
    public ApiResponse<List<OperatingHoursDto>> getOperatingHours(@PathVariable UUID id) {
        return ApiResponse.success(fieldScheduleService.getSubFieldOperatingHours(id));
    }

    @Operation(summary = "Replace sub-field operating hour overrides", description = "Replaces the full 7-day custom weekly schedule for a sub-field.")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @PutMapping("/{id}/operating-hours")
    public ApiResponse<List<OperatingHoursDto>> replaceOperatingHours(
            @PathVariable UUID id,
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody List<OperatingHoursRequest> request) {
        return ApiResponse.success(
                "Sub-field operating hours updated successfully",
                fieldScheduleService.replaceSubFieldOperatingHours(id, currentUser.id(), currentUser.role(), request));
    }

    @Operation(summary = "Get sub-field closures", description = "Returns maintenance and temporary closures for a sub-field.")
    @GetMapping("/{id}/closures")
    public ApiResponse<List<FieldClosureDto>> getClosures(@PathVariable UUID id) {
        return ApiResponse.success(fieldScheduleService.getClosures(id));
    }

    @Operation(summary = "Create sub-field closures", description = "Creates a maintenance or temporary closure for each selected sub-field ID. The entire request is rejected when any selected sub-field has a pending or confirmed booking in the date range.")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @PostMapping("/closures")
    public ApiResponse<List<FieldClosureDto>> createClosures(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody FieldClosureRequest request) {
        return ApiResponse.success(
                "Field closures created successfully",
                fieldScheduleService.createClosures(currentUser.id(), currentUser.role(), request));
    }

    @Operation(summary = "Update closure", description = "Updates a maintenance or temporary closure. The update is rejected when the sub-field has a pending or confirmed booking in the new date range.")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @PutMapping("/closures/{closureId}")
    public ApiResponse<FieldClosureDto> updateClosure(
            @PathVariable UUID closureId,
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody FieldClosureRequest request) {
        return ApiResponse.success(
                "Field closure updated successfully",
                fieldScheduleService.updateClosure(closureId, currentUser.id(), currentUser.role(), request));
    }

    @Operation(summary = "Delete closure", description = "Deletes a maintenance or temporary closure.")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    @DeleteMapping("/closures/{closureId}")
    public ApiResponse<Void> deleteClosure(
            @PathVariable UUID closureId,
            @CurrentUser UserPrincipal currentUser) {
        fieldScheduleService.deleteClosure(closureId, currentUser.id(), currentUser.role());
        return ApiResponse.success("Field closure deleted successfully", null);
    }
}
