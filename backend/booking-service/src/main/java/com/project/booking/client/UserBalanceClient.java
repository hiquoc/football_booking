package com.project.booking.client;

import com.project.common.dto.ApiResponse;
import com.project.common.dto.balance.BalanceDeductionRequest;
import com.project.common.dto.balance.BalanceDeductionResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserBalanceClient {
    private static final ParameterizedTypeReference<ApiResponse<BalanceDeductionResponse>> DEDUCTION_RESPONSE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient userServiceRestClient;

    public UserBalanceClient(@Qualifier("userServiceRestClient") RestClient userServiceRestClient) {
        this.userServiceRestClient = userServiceRestClient;
    }

    public BalanceDeductionResponse deduct(BalanceDeductionRequest request) {
        ApiResponse<BalanceDeductionResponse> response = userServiceRestClient.post()
                .uri("/internal/users/balance/deduct")
                .body(request)
                .retrieve()
                .body(DEDUCTION_RESPONSE);
        if (response == null || response.getData() == null) {
            throw new IllegalStateException("User service returned an empty balance deduction response");
        }
        return response.getData();
    }
}
