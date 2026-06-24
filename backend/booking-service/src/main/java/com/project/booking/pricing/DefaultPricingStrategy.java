package com.project.booking.pricing;

import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.response.SubFieldResponse;
import com.project.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import com.project.booking.dto.response.TimePriceRuleDto;

/**
 * Default pricing strategy: sum time-price-rule segments over the booking duration.
 * Future strategies (WeekendPricingStrategy, PeakPricingStrategy, etc.)
 * can implement PricingStrategy and be injected via qualifier or a PricingStrategyFactory.
 */
@Component
public class DefaultPricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal calculate(SubFieldResponse subField, CreateBookingRequest request) {
        LocalTime currentStartTime = request.getStartTime();
        LocalTime finalEndTime = request.getEndTime();
        BigDecimal totalPrice = BigDecimal.ZERO;

        List<TimePriceRuleDto> rules = subField.getTimePriceRules();
        
        if (rules == null || rules.isEmpty()) {
            throw new BadRequestException("Time price rules are not configured for this sub-field");
        }

        while (currentStartTime.isBefore(finalEndTime)) {
            TimePriceRuleDto applicableRule = null;
            for (TimePriceRuleDto rule : rules) {
                if (!currentStartTime.isBefore(rule.getStartTime()) && currentStartTime.isBefore(rule.getEndTime())) {
                    applicableRule = rule;
                    break;
                }
            }

            LocalTime segmentEndTime;
            BigDecimal priceToUse;
            
            if (applicableRule != null) {
                segmentEndTime = applicableRule.getEndTime().isBefore(finalEndTime) ? applicableRule.getEndTime() : finalEndTime;
                priceToUse = applicableRule.getHourlyPrice();
            } else {
                throw new BadRequestException("Time price rules do not cover the requested booking time");
            }

            long segmentMinutes = Duration.between(currentStartTime, segmentEndTime).toMinutes();
            if (segmentEndTime.equals(LocalTime.of(23, 59))) {
                segmentMinutes++;
            }
            BigDecimal segmentHours = BigDecimal.valueOf(segmentMinutes)
                    .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            
            totalPrice = totalPrice.add(priceToUse.multiply(segmentHours));
            currentStartTime = segmentEndTime;
        }

        return totalPrice.setScale(2, RoundingMode.HALF_UP);
    }
}
