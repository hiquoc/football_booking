package com.project.booking.service.impl;

import com.project.booking.dto.request.BookingConfigRequest;
import com.project.booking.dto.response.BookingConfigResponse;
import com.project.booking.entity.BookingConfig;
import com.project.booking.repository.BookingConfigRepository;
import com.project.booking.service.BookingConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingConfigServiceImpl implements BookingConfigService {

    private final BookingConfigRepository repository;
    private volatile BookingConfig cachedConfig;

    @PostConstruct
    @Transactional
    public void load() {
        cachedConfig = repository.findByActiveTrue()
                .orElseGet(() -> repository.save(BookingConfig.builder()
                        .bookingFee(0L)
                        .refundBeforeHours(24)
                        .refundEnabled(true)
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    @Override
    public BookingConfig getConfig() {
        BookingConfig config = cachedConfig;
        if (config == null) {
            throw new IllegalStateException("Booking configuration has not been loaded");
        }
        return config;
    }

    @Override
    public BookingConfigResponse getCurrent() {
        return toResponse(getConfig());
    }

    @Override
    @Transactional
    public BookingConfigResponse update(BookingConfigRequest request) {
        BookingConfig config = repository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Active booking configuration is missing"));
        config.setBookingFee(request.bookingFee());
        config.setRefundBeforeHours(request.refundBeforeHours());
        config.setRefundEnabled(request.refundEnabled());
        config.setUpdatedAt(LocalDateTime.now());
        BookingConfig saved = repository.saveAndFlush(config);
        cachedConfig = saved;
        return toResponse(saved);
    }

    private BookingConfigResponse toResponse(BookingConfig config) {
        return new BookingConfigResponse(
                config.getId(),
                config.getBookingFee(),
                config.getRefundBeforeHours(),
                config.getRefundEnabled(),
                config.getCreatedAt(),
                config.getUpdatedAt());
    }
}
