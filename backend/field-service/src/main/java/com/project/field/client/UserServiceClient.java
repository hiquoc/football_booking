package com.project.field.client;

import com.project.common.dto.ApiResponse;
import com.project.field.config.FeignConfig;
import com.project.field.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(
        name = "user-service",
        configuration = FeignConfig.class
)
public interface UserServiceClient {

    @GetMapping("/api/v1/users/internal/{id}")
    ApiResponse<UserDto> getUserProfile(@PathVariable("id") UUID id);

    @PutMapping("/api/v1/users/internal/{id}/role")
    ApiResponse<UserDto> changeUserRoleInternal(@PathVariable("id") UUID id, @RequestParam("userType") String userType);
}
