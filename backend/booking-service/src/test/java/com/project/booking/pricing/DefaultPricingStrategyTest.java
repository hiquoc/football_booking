package com.project.booking.pricing;

import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.dto.response.TimePriceRuleDto;
import com.project.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Test
    void calculatesBookingsThatSpanMidnight() {
        SubFieldResponse subField = SubFieldResponse.builder().timePriceRules(List.of(
                rule(18, 0, 2, 0, "180000")
        )).build();
        LocalDate bookingDate = LocalDate.now().plusDays(1);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .startDateTime(LocalDateTime.of(bookingDate, LocalTime.of(18, 0)))
                .endDateTime(LocalDateTime.of(bookingDate.plusDays(1), LocalTime.of(2, 0)))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(2, 0))
                .build();

        assertEquals(new BigDecimal("1440000.00"), strategy.calculate(subField, request));
    }

    @Test
    void treatsRuleEndingAt2359AsMidnight() {
        SubFieldResponse subField = SubFieldResponse.builder().timePriceRules(List.of(
                rule(18, 0, 23, 59, "180000")
        )).build();
        LocalDate bookingDate = LocalDate.now().plusDays(1);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .startDateTime(LocalDateTime.of(bookingDate, LocalTime.of(22, 30)))
                .endDateTime(LocalDateTime.of(bookingDate.plusDays(1), LocalTime.MIDNIGHT))
                .startTime(LocalTime.of(22, 30))
                .endTime(LocalTime.MIDNIGHT)
                .build();

        assertEquals(new BigDecimal("270000.00"), strategy.calculate(subField, request));
    }

    @Test
    void pricesAcrossMidnightWhenRulesAreSplitAt2359And0000() {
        SubFieldResponse subField = SubFieldResponse.builder().timePriceRules(List.of(
                rule(18, 0, 23, 59, "180000"),
                rule(0, 0, 2, 0, "240000")
        )).build();
        LocalDate bookingDate = LocalDate.now().plusDays(1);
        CreateBookingRequest request = CreateBookingRequest.builder()
                .startDateTime(LocalDateTime.of(bookingDate, LocalTime.of(22, 30)))
                .endDateTime(LocalDateTime.of(bookingDate.plusDays(1), LocalTime.of(0, 30)))
                .startTime(LocalTime.of(22, 30))
                .endTime(LocalTime.of(0, 30))
                .build();

        assertEquals(new BigDecimal("390000.00"), strategy.calculate(subField, request));
    }

    private TimePriceRuleDto rule(int startHour, int startMinute, int endHour, int endMinute, String price) {
        return TimePriceRuleDto.builder()
                .startTime(LocalTime.of(startHour, startMinute))
                .endTime(LocalTime.of(endHour, endMinute))
                .hourlyPrice(new BigDecimal(price))
                .build();
    }
}
