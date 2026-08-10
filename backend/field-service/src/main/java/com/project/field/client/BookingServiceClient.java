package com.project.field.client;

import com.project.common.dto.ApiResponse;
import com.project.field.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

@FeignClient(name = "booking-service", configuration = FeignConfig.class)
public interface BookingServiceClient {

    @GetMapping("/api/v1/bookings/internal/conflicts")
    ApiResponse<Boolean> hasBookingConflicts(
            @RequestParam("subFieldIds") Collection<UUID> subFieldIds,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate);

    @GetMapping("/api/v1/bookings/internal/completed-at-field")
    ApiResponse<Boolean> hasCompletedBookingAtField(
            @RequestParam("userId") UUID userId,
            @RequestParam("fieldId") UUID fieldId);
}
