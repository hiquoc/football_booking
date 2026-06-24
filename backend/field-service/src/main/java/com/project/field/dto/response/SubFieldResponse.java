package com.project.field.dto.response;

import com.project.common.enums.SubFieldType;
import com.project.common.enums.SportType;
import com.project.field.dto.TimePriceRuleDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Internal response sent from field-service to booking-service via Feign.
 * Contains all data the booking service needs to validate and price a booking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubFieldResponse {
    private UUID id;
    private UUID fieldId;
    private String fieldName;
    private String name;
    private SportType sportType;
    private SubFieldType subFieldType;
    private String status;
    private UUID ownerId;
    private List<TimePriceRuleDto> timePriceRules;

    // ── Per-subfield booking constraints ──────────────────────────────────────
    /** Minimum allowed booking duration in minutes. Null = use system default. */
    private Integer minimumBookingDurationMinutes;
    /** Maximum allowed booking duration in minutes. Null = use system default. */
    private Integer maximumBookingDurationMinutes;
    /** Optional start-time interval policy in minutes. */
    private Integer bookingIntervalMinutes;
}
