package com.project.booking.service.impl;

import com.project.booking.dto.request.BookingConfigRequest;
import com.project.booking.entity.BookingConfig;
import com.project.booking.repository.BookingConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingConfigServiceImplTest {

    @Mock
    private BookingConfigRepository repository;

    @InjectMocks
    private BookingConfigServiceImpl service;

    @Test
    void loadCachesActiveConfigurationInMemory() {
        BookingConfig config = config(0L, 24, true);
        when(repository.findByActiveTrue()).thenReturn(Optional.of(config));

        service.load();

        assertEquals(0L, service.getConfig().getBookingFee());
        assertEquals(24, service.getConfig().getRefundBeforeHours());
    }

    @Test
    void updatePersistsConfigurationAndReplacesCachedReference() {
        BookingConfig initial = config(0L, 24, true);
        BookingConfig loadedForUpdate = config(0L, 24, true);
        when(repository.findByActiveTrue())
                .thenReturn(Optional.of(initial))
                .thenReturn(Optional.of(loadedForUpdate));
        when(repository.saveAndFlush(loadedForUpdate)).thenReturn(loadedForUpdate);

        service.load();
        BookingConfig cachedBeforeUpdate = service.getConfig();

        service.update(new BookingConfigRequest(5000L, 12, false));

        assertNotSame(cachedBeforeUpdate, service.getConfig());
        assertEquals(5000L, service.getConfig().getBookingFee());
        assertEquals(12, service.getConfig().getRefundBeforeHours());
        assertEquals(false, service.getConfig().getRefundEnabled());
    }

    private BookingConfig config(long bookingFee, int refundBeforeHours, boolean refundEnabled) {
        return BookingConfig.builder()
                .id(UUID.randomUUID())
                .bookingFee(bookingFee)
                .refundBeforeHours(refundBeforeHours)
                .refundEnabled(refundEnabled)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
