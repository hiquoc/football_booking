package com.project.booking.mapper;

import com.project.booking.dto.response.BookingResponse;
import com.project.booking.dto.response.MatchResultResponse;
import com.project.booking.dto.response.SubFieldResponse;
import com.project.booking.entity.Booking;
import com.project.booking.entity.MatchResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    // Explicitly qualify all fields that exist in both Booking and SubFieldResponse
    @Mapping(target = "id",            source = "booking.id")
    @Mapping(target = "bookingCode",   source = "booking.bookingCode")
    @Mapping(target = "clientId",      source = "booking.clientId")
    @Mapping(target = "clientName", ignore = true)
    @Mapping(target = "clientPhoneNumber", ignore = true)
    @Mapping(target = "clientAvatarUrl", ignore = true)
    @Mapping(target = "opponentName", ignore = true)
    @Mapping(target = "opponentPhoneNumber", ignore = true)
    @Mapping(target = "subFieldId",    source = "booking.subFieldId")
    @Mapping(target = "ownerId",       source = "booking.ownerId")
    @Mapping(target = "bookingDate",   source = "booking.bookingDate")
    @Mapping(target = "startDateTime", source = "booking.startDateTime")
    @Mapping(target = "endDateTime",   source = "booking.endDateTime")
    @Mapping(target = "startTime",     source = "booking.startTime")
    @Mapping(target = "endTime",       source = "booking.endTime")
    @Mapping(target = "durationMinutes", source = "booking.durationMinutes")
    @Mapping(target = "pricePerHour",  source = "booking.pricePerHour")
    @Mapping(target = "subFieldPrice", source = "booking.subFieldPrice")
    @Mapping(target = "bookingPrice", source = "booking.bookingPrice")
    @Mapping(target = "platformBookingFee", source = "booking.platformBookingFee")
    @Mapping(target = "bookingType", source = "booking.bookingType")
    @Mapping(target = "paymentMethod", source = "booking.paymentMethod")
    @Mapping(target = "status",        source = "booking.status")
    @Mapping(target = "paymentStatus", source = "booking.paymentStatus")
    @Mapping(target = "note",          source = "booking.note")
    @Mapping(target = "cancellationReason", source = "booking.cancellationReason")
    @Mapping(target = "cancelledAt", source = "booking.cancelledAt")
    @Mapping(target = "paymentExpiresAt", source = "booking.paymentExpiresAt")
    @Mapping(target = "cancelledBy", source = "booking.cancelledBy")
    @Mapping(target = "matchResult", ignore = true)
    @Mapping(target = "createdAt",     source = "booking.createdAt")
    @Mapping(target = "updatedAt",     source = "booking.updatedAt")
    @Mapping(target = "subFieldName",  source = "subField.name")
    @Mapping(target = "fieldId",       source = "subField.fieldId")
    @Mapping(target = "fieldName",     source = "subField.fieldName")
    BookingResponse toResponse(Booking booking, SubFieldResponse subField);

    @Mapping(target = "subFieldName", source = "subField.name")
    @Mapping(target = "fieldName", source = "subField.fieldName")
    @Mapping(target = "clientName", ignore = true)
    @Mapping(target = "clientPhoneNumber", ignore = true)
    @Mapping(target = "clientAvatarUrl", ignore = true)
    @Mapping(target = "opponentName", ignore = true)
    @Mapping(target = "opponentPhoneNumber", ignore = true)
    @Mapping(target = "fieldId", source = "subField.fieldId")
    @Mapping(target = "matchResult", ignore = true)
    BookingResponse toResponse(Booking booking);

    @Mapping(target = "result", source = "winningTeam")
    MatchResultResponse toMatchResultResponse(MatchResult matchResult);
}
