package com.project.booking.pricing;

import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.dto.response.TimePriceRuleDto;
import com.project.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultPricingStrategyTest {

    private final DefaultPricingStrategy strategy = new DefaultPricingStrategy();

    @Test
    void calculatesAcrossPriceBoundariesAndRoundsUpToNearestThousand() {
        SubFieldResponse subField = SubFieldResponse.builder().timePriceRules(List.of(
                rule(8, 0, 17, 0, "220000"),
                rule(17, 0, 23, 0, "250000")
        )).build();
        CreateBookingRequest request = CreateBookingRequest.builder()
                .startTime(LocalTime.of(16, 0))
                .endTime(LocalTime.of(17, 30))
                .build();

        assertEquals(new BigDecimal("345000.00"), strategy.calculate(subField, request));
    }

    @Test
    void rejectsAnyGapInConfiguredPriceRules() {
        SubFieldResponse subField = SubFieldResponse.builder().timePriceRules(List.of(
                rule(8, 0, 17, 0, "220000")
        )).build();
        CreateBookingRequest request = CreateBookingRequest.builder()
                .startTime(LocalTime.of(16, 30))
                .endTime(LocalTime.of(17, 30))
                .build();

        assertThrows(BadRequestException.class, () -> strategy.calculate(subField, request));
    }

    private TimePriceRuleDto rule(int startHour, int startMinute, int endHour, int endMinute, String price) {
        return TimePriceRuleDto.builder()
                .startTime(LocalTime.of(startHour, startMinute))
                .endTime(LocalTime.of(endHour, endMinute))
                .hourlyPrice(new BigDecimal(price))
                .build();
    }
}
