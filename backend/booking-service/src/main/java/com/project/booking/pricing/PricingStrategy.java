package com.project.booking.pricing;

import com.project.booking.dto.request.CreateBookingRequest;
import com.project.booking.dto.response.SubFieldResponse;

import java.math.BigDecimal;

/**
 * Strategy interface for booking price calculation.
 * Implementations can apply different pricing rules
 * (e.g. peak hours, weekends, holidays, promotions).
 */
public interface PricingStrategy {

    /**
     * Calculates the total price for a booking.
     *
     * @param subField the subfield containing the base price per hour
     * @param request  the booking request containing date and time range
     * @return the calculated total amount
     */
    BigDecimal calculate(SubFieldResponse subField, CreateBookingRequest request);
}
