package com.project.field.service.impl;

import com.project.field.entity.FieldOperatingHours;
import com.project.field.entity.SubField;
import com.project.field.entity.SubFieldOperatingHours;
import com.project.field.entity.TimePriceRule;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class OperatingHoursPriceRuleSynchronizer {
    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final LocalTime END_OF_DAY_TIME = LocalTime.of(23, 59);

    public List<SubField> synchronizeFieldHours(List<SubField> subFields, List<FieldOperatingHours> operatingHours) {
        boolean[] targetCoverage = fieldOperatingHoursCoverage(operatingHours);
        List<SubField> changed = new ArrayList<>();
        for (SubField subField : subFields) {
            if (fillMissingPriceRuleCoverage(subField, targetCoverage)) {
                changed.add(subField);
            }
        }
        return changed;
    }

    public boolean synchronizeSubFieldHours(SubField subField, List<SubFieldOperatingHours> operatingHours) {
        return fillMissingPriceRuleCoverage(subField, subFieldOperatingHoursCoverage(operatingHours));
    }

    private boolean[] fieldOperatingHoursCoverage(List<FieldOperatingHours> operatingHours) {
        boolean[] coverage = new boolean[MINUTES_PER_DAY];
        for (FieldOperatingHours hours : operatingHours) {
            if (Boolean.TRUE.equals(hours.getClosed())) {
                continue;
            }
            if (Boolean.TRUE.equals(hours.getOpen24Hours())) {
                markCoverage(coverage, 0, MINUTES_PER_DAY);
                continue;
            }
            if (hours.getOpenTime() == null || hours.getCloseTime() == null) {
                continue;
            }
            markCoverage(coverage, toMinuteOfDay(hours.getOpenTime()), endMinuteOfDay(hours.getCloseTime()));
        }
        return coverage;
    }

    private boolean[] subFieldOperatingHoursCoverage(List<SubFieldOperatingHours> operatingHours) {
        boolean[] coverage = new boolean[MINUTES_PER_DAY];
        for (SubFieldOperatingHours hours : operatingHours) {
            if (Boolean.TRUE.equals(hours.getClosed())) {
                continue;
            }
            if (Boolean.TRUE.equals(hours.getOpen24Hours())) {
                markCoverage(coverage, 0, MINUTES_PER_DAY);
                continue;
            }
            if (hours.getOpenTime() == null || hours.getCloseTime() == null) {
                continue;
            }
            markCoverage(coverage, toMinuteOfDay(hours.getOpenTime()), endMinuteOfDay(hours.getCloseTime()));
        }
        return coverage;
    }

    private boolean fillMissingPriceRuleCoverage(SubField subField, boolean[] targetCoverage) {
        if (subField.getTimePriceRules() == null || subField.getTimePriceRules().isEmpty()) {
            return false;
        }
        boolean[] existingCoverage = new boolean[MINUTES_PER_DAY];
        subField.getTimePriceRules().forEach(rule ->
                markCoverage(existingCoverage, toMinuteOfDay(rule.getStartTime()), endMinuteOfDay(rule.getEndTime())));

        boolean updated = false;
        int minute = 0;
        while (minute < MINUTES_PER_DAY) {
            if (!targetCoverage[minute] || existingCoverage[minute]) {
                minute++;
                continue;
            }
            int start = minute;
            while (minute < MINUTES_PER_DAY && targetCoverage[minute] && !existingCoverage[minute]) {
                minute++;
            }
            int end = minute;
            fillMissingRange(subField, start, end);
            markCoverage(existingCoverage, start, end);
            updated = true;
        }
        return updated;
    }

    private void fillMissingRange(SubField subField, int startMinute, int endMinute) {
        TimePriceRule previousRule = ruleEndingAt(subField.getTimePriceRules(), startMinute);
        if (previousRule != null) {
            previousRule.setEndTime(toRuleEndTime(endMinute));
            return;
        }

        TimePriceRule nextRule = ruleStartingAt(subField.getTimePriceRules(), endMinute);
        if (nextRule != null) {
            nextRule.setStartTime(toLocalTime(startMinute));
            return;
        }

        TimePriceRule sourceRule = nearestPreviousRule(subField.getTimePriceRules(), startMinute);
        TimePriceRule added = TimePriceRule.builder()
                .subField(subField)
                .startTime(toLocalTime(startMinute))
                .endTime(toRuleEndTime(endMinute))
                .hourlyPrice(sourceRule.getHourlyPrice())
                .build();
        subField.getTimePriceRules().add(added);
    }

    private TimePriceRule ruleEndingAt(List<TimePriceRule> rules, int minute) {
        return rules.stream()
                .filter(rule -> endMinuteOfDay(rule.getEndTime()) == minute)
                .findFirst()
                .orElse(null);
    }

    private TimePriceRule ruleStartingAt(List<TimePriceRule> rules, int minute) {
        return rules.stream()
                .filter(rule -> toMinuteOfDay(rule.getStartTime()) == minute)
                .findFirst()
                .orElse(null);
    }

    private TimePriceRule nearestPreviousRule(List<TimePriceRule> rules, int startMinute) {
        int previousMinute = (startMinute + MINUTES_PER_DAY - 1) % MINUTES_PER_DAY;
        return rules.stream()
                .filter(rule -> coversMinute(rule, previousMinute))
                .findFirst()
                .orElseGet(() -> rules.stream()
                        .max(Comparator.comparingInt(rule -> endMinuteOfDay(rule.getEndTime())))
                        .orElseThrow());
    }

    private boolean coversMinute(TimePriceRule rule, int minute) {
        int start = toMinuteOfDay(rule.getStartTime());
        int end = endMinuteOfDay(rule.getEndTime());
        if (end > start) {
            return minute >= start && minute < end;
        }
        return minute >= start || minute < end;
    }

    private void markCoverage(boolean[] coverage, int start, int end) {
        int length = end > start ? end - start : MINUTES_PER_DAY - start + end;
        for (int offset = 0; offset < length; offset++) {
            coverage[(start + offset) % MINUTES_PER_DAY] = true;
        }
    }

    private int toMinuteOfDay(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }

    private int endMinuteOfDay(LocalTime time) {
        return END_OF_DAY_TIME.equals(time) ? MINUTES_PER_DAY : toMinuteOfDay(time);
    }

    private LocalTime toLocalTime(int minute) {
        return LocalTime.of(minute / 60, minute % 60);
    }

    private LocalTime toRuleEndTime(int minute) {
        return minute == MINUTES_PER_DAY ? END_OF_DAY_TIME : toLocalTime(minute);
    }
}
