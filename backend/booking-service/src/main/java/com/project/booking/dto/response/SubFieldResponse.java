package com.project.booking.dto.response;

import com.project.common.enums.SubFieldType;
import com.project.common.enums.SportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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
    private Boolean active;
    private UUID ownerId;
    private LocalTime openTime;
    private LocalTime closeTime;
    private List<TimePriceRuleDto> timePriceRules;

    // ── Per-subfield booking constraints ──────────────────────────────────────
    /** Minimum allowed booking duration in minutes. Null = use system default. */
    private Integer minimumBookingDurationMinutes;
    /** Maximum allowed booking duration in minutes. Null = use system default. */
    private Integer maximumBookingDurationMinutes;
    /** Legacy field-service setting. Booking start validation is enforced by booking-service policy. */
    private Integer bookingIntervalMinutes;
    private Boolean hasRecurring;
}
