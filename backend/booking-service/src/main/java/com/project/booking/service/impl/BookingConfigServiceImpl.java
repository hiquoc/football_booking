package com.project.booking.service.impl;

import com.project.booking.dto.request.BookingConfigRequest;
import com.project.booking.dto.response.BookingConfigResponse;
import com.project.booking.entity.BookingConfig;
import com.project.booking.repository.BookingConfigRepository;
import com.project.booking.service.BookingConfigService;
import com.project.common.cache.CacheKeys;
import com.project.common.cache.CacheNames;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingConfigServiceImpl implements BookingConfigService {

    private final BookingConfigRepository repository;

    @PostConstruct
    @Transactional
    public void load() {
        getOrCreateActiveConfig();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.BOOKING_CONFIG, key = CacheKeys.BOOKING_CONFIG, sync = true)
    public BookingConfig getConfig() {
        return getOrCreateActiveConfig();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.BOOKING_CONFIG, key = "'current-response'", sync = true)
    public BookingConfigResponse getCurrent() {
        return toResponse(getConfig());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheNames.BOOKING_CONFIG, allEntries = true)
    public BookingConfigResponse update(BookingConfigRequest request) {
        BookingConfig config = repository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Active booking configuration is missing"));
        config.setFirstBookingFee(request.firstBookingFee());
        config.setNotFirstBookingFee(request.notFirstBookingFee());
        config.setRefundBeforeHours(request.refundBeforeHours());
        config.setRefundEnabled(request.refundEnabled());
        config.setUpdatedAt(LocalDateTime.now());
        BookingConfig saved = repository.saveAndFlush(config);
        return toResponse(saved);
    }

    private BookingConfig getOrCreateActiveConfig() {
        return repository.findByActiveTrue()
                .orElseGet(() -> repository.save(BookingConfig.builder()
                        .firstBookingFee(5_000L)
                        .notFirstBookingFee(1_000L)
                        .refundBeforeHours(24)
                        .refundEnabled(true)
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    private BookingConfigResponse toResponse(BookingConfig config) {
        return new BookingConfigResponse(
                config.getId(),
                config.getFirstBookingFee(),
                config.getNotFirstBookingFee(),
                config.getRefundBeforeHours(),
                config.getRefundEnabled(),
                config.getCreatedAt(),
                config.getUpdatedAt());
    }
}
