package com.project.common.events.field;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OperatingHoursChangedEvent(
        String entityType,
        UUID entityId,
        UUID fieldId,
        List<OperatingHoursSnapshot> previousOperatingHours,
        List<OperatingHoursSnapshot> operatingHours,
        Instant occurredAt,
        UUID eventId
) {
}
