package com.project.booking.client;

import com.project.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;
import java.util.List;

@Component
public class FieldManagementClient {
    private static final ParameterizedTypeReference<ApiResponse<Boolean>> BOOLEAN_RESPONSE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<ApiResponse<List<UUID>>> UUID_LIST_RESPONSE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient fieldServiceRestClient;

    public FieldManagementClient(@Qualifier("fieldServiceRestClient") RestClient fieldServiceRestClient) {
        this.fieldServiceRestClient = fieldServiceRestClient;
    }

    public boolean canManageField(UUID userId, String role, UUID fieldId) {
        ApiResponse<Boolean> response = fieldServiceRestClient.get()
                .uri("/api/v1/fields/{fieldId}/managers/me", fieldId)
                .header(com.project.common.constants.GlobalConstants.HEADER_USER_ID, userId.toString())
                .header(com.project.common.constants.GlobalConstants.HEADER_USER_ROLE, role)
                .retrieve()
                .body(BOOLEAN_RESPONSE);
        return response != null && Boolean.TRUE.equals(response.getData());
    }

    public List<UUID> assignedFieldIds(UUID employeeId) {
        ApiResponse<List<UUID>> response = fieldServiceRestClient.get()
                .uri("/api/v1/fields/employee/assigned-ids")
                .header(com.project.common.constants.GlobalConstants.HEADER_USER_ID, employeeId.toString())
                .header(com.project.common.constants.GlobalConstants.HEADER_USER_ROLE, "EMPLOYEE")
                .retrieve()
                .body(UUID_LIST_RESPONSE);
        return response == null || response.getData() == null ? List.of() : response.getData();
    }
}
