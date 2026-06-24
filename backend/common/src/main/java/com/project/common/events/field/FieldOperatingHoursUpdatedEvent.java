package com.project.common.events.field;

import java.util.List;
import java.util.UUID;

public record FieldOperatingHoursUpdatedEvent(
        UUID fieldId,
        List<OperatingHoursSnapshot> operatingHours
) {
}
