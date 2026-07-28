package com.project.user.controller;

import com.project.common.dto.ApiResponse;
import com.project.common.dto.balance.BalanceDeductionRequest;
import com.project.common.dto.balance.BalanceDeductionResponse;
import com.project.user.service.UserBalanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users/balance")
@RequiredArgsConstructor
public class InternalUserBalanceController {
    private final UserBalanceService userBalanceService;

    @PostMapping("/deduct")
    public ApiResponse<BalanceDeductionResponse> deduct(@Valid @RequestBody BalanceDeductionRequest request) {
        return ApiResponse.success("Balance deduction processed", userBalanceService.deductSync(request));
    }
}
