package com.project.booking.controller;

import com.project.booking.dto.request.BookingConfigRequest;
import com.project.booking.dto.response.BookingConfigResponse;
import com.project.booking.service.BookingConfigService;
import com.project.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/booking-config")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BookingConfigController {

    private final BookingConfigService bookingConfigService;

    @GetMapping
    public ApiResponse<BookingConfigResponse> get() {
        return ApiResponse.success(bookingConfigService.getCurrent());
    }

    @PutMapping
    public ApiResponse<BookingConfigResponse> update(@Valid @RequestBody BookingConfigRequest request) {
        return ApiResponse.success("Booking configuration updated", bookingConfigService.update(request));
    }
}
