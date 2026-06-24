package com.project.field.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.common.dto.ApiResponse;
import com.project.field.dto.FieldTypeDto;
import com.project.field.dto.FieldTypeRequest;
import com.project.field.service.FieldTypeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/field-types")
@RequiredArgsConstructor
@Tag(name = "Field Types", description = "Manage configurable sport categories (e.g. Football, Badminton). Admin-only for write operations.")
public class FieldTypeController {

    private final FieldTypeService fieldTypeService;

    @Operation(
            summary = "Create a field type",
            description = "Creates a new sport category. The name must be a SportType enum value. The defaultBookingDurationMinutes describes the default duration policy for this sport type.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = FieldTypeRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "name": "FOOTBALL",
                                      "defaultBookingDurationMinutes": 90,
                                      "description": "11-a-side or 5-a-side football",
                                      "active": true
                                    }
                                    """)
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Field type created",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "message": "Field type created successfully",
                                      "data": { "id": 1, "name": "FOOTBALL", "allowedSubFieldTypes": ["FOOTBALL_5V5", "FOOTBALL_7V7", "FOOTBALL_11V11"], "defaultBookingDurationMinutes": 90, "active": true }
                                    }
                                    """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<FieldTypeDto> create(@Valid @RequestBody FieldTypeRequest request) {
        return ApiResponse.success("Field type created successfully", fieldTypeService.create(request));
    }

    @Operation(summary = "Update a field type", description = "Updates the sport name, duration, or active status of an existing field type. The name must be a SportType enum value.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Field type updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Field type not found", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<FieldTypeDto> update(@PathVariable Long id, @Valid @RequestBody FieldTypeRequest request) {
        return ApiResponse.success("Field type updated successfully", fieldTypeService.update(id, request));
    }

    @Operation(summary = "Delete a field type", description = "Removes a field type. This operation should be used with caution if sub-fields already reference it.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Field type deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Field type not found", content = @Content)
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        fieldTypeService.delete(id);
        return ApiResponse.success("Field type deleted successfully", null);
    }

    @Operation(summary = "Get all field types", description = "Returns the full list of sport categories available in the system.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List returned successfully")
    @GetMapping
    public ApiResponse<List<FieldTypeDto>> getAll() {
        return ApiResponse.success(fieldTypeService.getAll());
    }
}
