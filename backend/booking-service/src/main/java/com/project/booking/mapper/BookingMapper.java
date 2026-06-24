package com.project.booking.mapper;

import com.project.booking.dto.response.BookingResponse;
import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    // Explicitly qualify all fields that exist in both Booking and SubFieldResponse
    @Mapping(target = "id",            source = "booking.id")
    @Mapping(target = "bookingCode",   source = "booking.bookingCode")
    @Mapping(target = "clientId",      source = "booking.clientId")
    @Mapping(target = "subFieldId",    source = "booking.subFieldId")
    @Mapping(target = "ownerId",       source = "booking.ownerId")
    @Mapping(target = "bookingDate",   source = "booking.bookingDate")
    @Mapping(target = "startTime",     source = "booking.startTime")
    @Mapping(target = "endTime",       source = "booking.endTime")
    @Mapping(target = "durationMinutes", source = "booking.durationMinutes")
    @Mapping(target = "pricePerHour",  source = "booking.pricePerHour")
    @Mapping(target = "totalAmount",   source = "booking.totalAmount")
    @Mapping(target = "status",        source = "booking.status")
    @Mapping(target = "note",          source = "booking.note")
    @Mapping(target = "cancellationReason", source = "booking.cancellationReason")
    @Mapping(target = "cancelledAt", source = "booking.cancelledAt")
    @Mapping(target = "cancelledBy", source = "booking.cancelledBy")
    @Mapping(target = "createdAt",     source = "booking.createdAt")
    @Mapping(target = "updatedAt",     source = "booking.updatedAt")
    @Mapping(target = "subFieldName",  source = "subField.name")
    @Mapping(target = "fieldName",     source = "subField.fieldName")
    BookingResponse toResponse(Booking booking, SubFieldResponse subField);

    @Mapping(target = "subFieldName", ignore = true)
    @Mapping(target = "fieldName", ignore = true)
    BookingResponse toResponse(Booking booking);
}
