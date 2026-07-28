package com.project.common.events.field;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TimePriceRulesChangedEvent(
        UUID subFieldId,
        UUID fieldId,
        List<TimePriceRuleSnapshot> timePriceRules,
        Instant occurredAt,
        UUID eventId
) {
}
