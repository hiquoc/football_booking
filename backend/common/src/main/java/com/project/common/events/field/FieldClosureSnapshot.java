package com.project.common.events.field;

import java.time.LocalDate;
import java.util.UUID;

public record FieldClosureSnapshot(
        UUID closureId,
        UUID subFieldId,
        LocalDate startDate,
        LocalDate endDate,
        String reason
) {
}
