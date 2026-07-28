package com.project.field.controller;

import com.project.common.dto.ApiResponse;
import com.project.field.dto.SubFieldFilterOptionDto;
import com.project.field.service.SubFieldService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subfields")
@RequiredArgsConstructor
public class SubFieldFilterOptionsController {
    private final SubFieldService subFieldService;

    @Operation(summary = "Get sub-field filter options", description = "Returns lightweight active sub-field options for booking filters.")
    @GetMapping("/filter-options")
    public ApiResponse<List<SubFieldFilterOptionDto>> getFilterOptions(
            @RequestParam(required = false) String search) {
        return ApiResponse.success(subFieldService.getFilterOptions(search));
    }
}
