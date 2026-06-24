package com.project.common.events.field;

import java.util.UUID;

public record FieldClosureDeletedEvent(UUID closureId, UUID subFieldId) {
}
