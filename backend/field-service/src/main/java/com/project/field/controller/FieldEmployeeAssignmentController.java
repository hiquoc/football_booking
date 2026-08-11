package com.project.field.controller;

import com.project.common.dto.ApiResponse;
import com.project.common.dto.PageResponse;
import com.project.common.security.CurrentUser;
import com.project.common.security.UserPrincipal;
import com.project.field.dto.FieldDto;
import com.project.field.dto.FieldEmployeeAssignmentRequest;
import com.project.field.dto.FieldEmployeeDto;
import com.project.field.service.FieldEmployeeAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fields")
@RequiredArgsConstructor
public class FieldEmployeeAssignmentController {
    private final FieldEmployeeAssignmentService assignmentService;

    @PreAuthorize("hasAnyRole('OWNER','EMPLOYEE')")
    @GetMapping("/{fieldId}/employees")
    public ApiResponse<List<FieldEmployeeDto>> getFieldEmployees(
            @PathVariable UUID fieldId,
            @CurrentUser UserPrincipal user) {
        return ApiResponse.success(assignmentService.getFieldEmployees(user, fieldId));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/{fieldId}/employees")
    public ApiResponse<FieldEmployeeDto> assignEmployee(
            @PathVariable UUID fieldId,
            @CurrentUser UserPrincipal user,
            @Valid @RequestBody FieldEmployeeAssignmentRequest request) {
        return ApiResponse.success("Employee assigned successfully",
                assignmentService.assign(user.id(), fieldId, request.getEmployeeId()));
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{fieldId}/employees/{employeeId}")
    public ApiResponse<Void> removeEmployee(
            @PathVariable UUID fieldId,
            @PathVariable UUID employeeId,
            @CurrentUser UserPrincipal user) {
        assignmentService.remove(user.id(), fieldId, employeeId);
        return ApiResponse.success("Employee assignment removed", null);
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @PageableAsQueryParam
    @GetMapping("/employee/assigned")
    public ApiResponse<PageResponse<FieldDto>> getAssignedFields(
            @CurrentUser UserPrincipal user,
            Pageable pageable) {
        return ApiResponse.success(assignmentService.getAssignedFields(user.id(), pageable));
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @GetMapping("/employee/assigned-ids")
    public ApiResponse<List<UUID>> getAssignedFieldIds(@CurrentUser UserPrincipal user) {
        return ApiResponse.success(assignmentService.getAssignedFieldIds(user.id()));
    }

    @PreAuthorize("hasAnyRole('OWNER','EMPLOYEE')")
    @GetMapping("/{fieldId}/managers/me")
    public ApiResponse<Boolean> canManageField(
            @PathVariable UUID fieldId,
            @CurrentUser UserPrincipal user) {
        return ApiResponse.success(assignmentService.canManageField(user.id(), fieldId));
    }
}
