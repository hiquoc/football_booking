package com.project.common.events.field;

import java.util.List;
import java.util.UUID;

public record SubFieldOperatingHoursUpdatedEvent(
        UUID subFieldId,
        List<OperatingHoursSnapshot> operatingHours
) {
}
