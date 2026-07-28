package com.project.booking.pricing;

import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.response.SubFieldResponse;
import com.project.common.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private static final LocalTime END_OF_DAY_TIME = LocalTime.of(23, 59);

    @Override
    public BigDecimal calculate(SubFieldResponse subField, CreateBookingRequest request) {
        LocalDateTime currentStart = request.getStartDateTime() != null
                ? request.getStartDateTime()
                : LocalDate.now().atTime(request.getStartTime());
        LocalDateTime finalEnd = request.getEndDateTime() != null
                ? request.getEndDateTime()
                : sameDayOrNextDay(currentStart, request.getEndTime());
        BigDecimal totalPrice = BigDecimal.ZERO;

        List<TimePriceRuleDto> rules = subField.getTimePriceRules();
        
        if (rules == null || rules.isEmpty()) {
            throw new BadRequestException("Time price rules are not configured for this sub-field");
        }

        while (currentStart.isBefore(finalEnd)) {
            LocalTime currentTime = currentStart.toLocalTime();
            LocalDateTime uncoveredStart = currentStart;
            TimePriceRuleDto applicableRule = rules.stream()
                    .filter(rule -> isWithinRule(currentTime, rule))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException(
                            "Time price rules do not cover requested booking timestamp " + uncoveredStart
                                    + ". Configured rules: " + describeRules(rules)));

            LocalDateTime ruleEnd = ruleEndDateTime(currentStart, applicableRule.getEndTime());
            LocalDateTime segmentEnd = ruleEnd.isBefore(finalEnd) ? ruleEnd : finalEnd;

            long segmentMinutes = java.time.Duration.between(currentStart, segmentEnd).toMinutes();
            BigDecimal segmentHours = BigDecimal.valueOf(segmentMinutes)
                    .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            
            totalPrice = totalPrice.add(applicableRule.getHourlyPrice().multiply(segmentHours));
            currentStart = segmentEnd;
        }

        // Prices are displayed and collected in whole thousands of VND.
        return totalPrice.setScale(-3, RoundingMode.CEILING).setScale(2);
    }

    private LocalDateTime sameDayOrNextDay(LocalDateTime start, LocalTime endTime) {
        LocalDateTime end = LocalDateTime.of(start.toLocalDate(), endTime);
        return end.isAfter(start) ? end : end.plusDays(1);
    }

    private boolean isWithinRule(LocalTime time, TimePriceRuleDto rule) {
        LocalTime start = rule.getStartTime();
        LocalTime end = END_OF_DAY_TIME.equals(rule.getEndTime()) ? LocalTime.MIDNIGHT : rule.getEndTime();
        if (LocalTime.MIDNIGHT.equals(end) && !LocalTime.MIDNIGHT.equals(start)) {
            return !time.isBefore(start);
        }
        if (end.isAfter(start)) {
            return !time.isBefore(start) && time.isBefore(end);
        }
        return !time.isBefore(start) || time.isBefore(end);
    }

    private LocalDateTime ruleEndDateTime(LocalDateTime currentStart, LocalTime ruleEndTime) {
        LocalDateTime ruleEnd = LocalDateTime.of(currentStart.toLocalDate(), ruleEndTime);
        if (END_OF_DAY_TIME.equals(ruleEndTime)) {
            ruleEnd = ruleEnd.plusMinutes(1);
        }
        return ruleEnd.isAfter(currentStart) ? ruleEnd : ruleEnd.plusDays(1);
    }

    private String describeRules(List<TimePriceRuleDto> rules) {
        return rules.stream()
                .map(rule -> rule.getStartTime() + "-" + rule.getEndTime())
                .toList()
                .toString();
    }
}
