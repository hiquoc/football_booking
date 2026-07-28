package com.project.common.events.field;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FieldOperatingHoursUpdatedEvent(
        UUID fieldId,
        List<UUID> affectedSubFieldIds,
        List<OperatingHoursSnapshot> previousOperatingHours,
        List<OperatingHoursSnapshot> operatingHours,
        Instant occurredAt,
        UUID eventId
) {
    public FieldOperatingHoursUpdatedEvent(UUID fieldId, List<OperatingHoursSnapshot> operatingHours) {
        this(fieldId, List.of(), List.of(), operatingHours, Instant.now(), UUID.randomUUID());
    }
}
