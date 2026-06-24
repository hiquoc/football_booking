package com.project.common.events.field;

import com.project.common.enums.SubFieldType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SubFieldCreatedEvent(
        UUID subFieldId,
        UUID fieldId,
        String fieldName,
        String name,
        Boolean active,
        UUID ownerId,
        SubFieldType subFieldType,
        Integer minimumBookingDurationMinutes,
        Integer maximumBookingDurationMinutes,
        Integer bookingIntervalMinutes,
        List<TimePriceRuleSnapshot> timePriceRules
) {
}
