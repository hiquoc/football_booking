package com.project.common.events.field;

import java.time.LocalDate;
import java.util.UUID;

public record FieldClosureUpdatedEvent(
        UUID closureId,
        UUID subFieldId,
        LocalDate startDate,
        LocalDate endDate,
        String reason
) {
}
