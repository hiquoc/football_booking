package com.project.common.events.field;

import java.util.List;
import java.util.UUID;

public record FieldClosureCreatedEvent(
        UUID eventId,
        List<FieldClosureSnapshot> closures
) {
}
