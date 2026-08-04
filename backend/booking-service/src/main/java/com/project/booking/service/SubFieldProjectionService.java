package com.project.booking.service;

import com.project.booking.dto.response.SubFieldResponse;

import java.time.DayOfWeek;
import java.util.UUID;

public interface SubFieldProjectionService {
    SubFieldResponse getRequiredSubField(UUID subFieldId);

    ResolvedOperatingHours resolveOperatingHours(UUID subFieldId, UUID fieldId, DayOfWeek dayOfWeek);
}
