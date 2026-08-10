package com.project.booking.service.impl;

import com.project.booking.dto.request.BookingConfigRequest;
import com.project.booking.entity.BookingConfig;
import com.project.booking.repository.BookingConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingConfigServiceImplTest {

    @Mock
    private BookingConfigRepository repository;

    @InjectMocks
    private BookingConfigServiceImpl service;

    @Test
    void loadCachesActiveConfigurationInMemory() {
        BookingConfig config = config(5000L, 1000L, 24, true);
        when(repository.findByActiveTrue()).thenReturn(Optional.of(config));
        ReflectionTestUtils.setField(service, "maxBookingDaysInFuture", 45);

        service.load();

        assertEquals(5000L, service.getConfig().getFirstBookingFee());
        assertEquals(1000L, service.getConfig().getNotFirstBookingFee());
        assertEquals(24, service.getConfig().getRefundBeforeHours());
        assertEquals(45, service.getCurrent().maxBookingDaysInFuture());
    }

    @Test
    void updatePersistsConfiguration() {
        BookingConfig initial = config(5000L, 1000L, 24, true);
        BookingConfig loadedForUpdate = config(5000L, 1000L, 24, true);
        when(repository.findByActiveTrue())
                .thenReturn(Optional.of(initial))
                .thenReturn(Optional.of(loadedForUpdate));
        when(repository.saveAndFlush(loadedForUpdate)).thenReturn(loadedForUpdate);

        service.load();

        service.update(new BookingConfigRequest(6000L, 2000L, 12, false));

        assertEquals(6000L, service.getConfig().getFirstBookingFee());
        assertEquals(2000L, service.getConfig().getNotFirstBookingFee());
        assertEquals(12, service.getConfig().getRefundBeforeHours());
        assertEquals(false, service.getConfig().getRefundEnabled());
    }

    private BookingConfig config(long firstBookingFee, long notFirstBookingFee, int refundBeforeHours, boolean refundEnabled) {
        return BookingConfig.builder()
                .id(UUID.randomUUID())
                .firstBookingFee(firstBookingFee)
                .notFirstBookingFee(notFirstBookingFee)
                .refundBeforeHours(refundBeforeHours)
                .refundEnabled(refundEnabled)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
