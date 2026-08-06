package com.project.booking.mapper;

import com.project.booking.dto.response.RecurringBookingResponse;
import com.project.booking.entity.RecurringBooking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecurringBookingMapper {

    @Mapping(target = "fieldName", source = "subField.fieldName")
    @Mapping(target = "subFieldName", source = "subField.name")
    @Mapping(target = "nextMatchAt", ignore = true)
    @Mapping(target = "generatedDates", ignore = true)
    @Mapping(target = "occupiedDates", ignore = true)
    @Mapping(target = "firstBooking", ignore = true)
    @Mapping(target = "latestBooking", ignore = true)
    RecurringBookingResponse toResponse(RecurringBooking recurringBooking);
}
