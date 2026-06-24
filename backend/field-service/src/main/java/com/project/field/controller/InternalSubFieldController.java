package com.project.field.controller;

import com.project.common.dto.ApiResponse;
import com.project.field.dto.response.SubFieldResponse;
import com.project.field.service.SubFieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/sub-fields")
@RequiredArgsConstructor
public class InternalSubFieldController {

    private final SubFieldService subFieldService;

    @GetMapping("/{subFieldId}")
    public ApiResponse<SubFieldResponse> getSubFieldById(@PathVariable UUID subFieldId) {
        SubFieldResponse response = subFieldService.getInternalSubFieldResponse(subFieldId);
        return ApiResponse.success("Fetched sub-field successfully", response);
    }
}
