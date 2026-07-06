package com.project.booking.mapper;

import com.project.booking.dto.response.BookingResponse;
import com.project.booking.entity.Booking;
import com.project.booking.entity.SubFieldProjection;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingMapperTest {

    private final BookingMapper bookingMapper = Mappers.getMapper(BookingMapper.class);

    @Test
    void mapsFieldAndSubFieldNamesFromProjection() {
        SubFieldProjection subField = SubFieldProjection.builder()
                .id(UUID.randomUUID())
                .fieldName("Sân bóng Trung Tâm")
                .name("Sân 5A")
                .build();
        Booking booking = Booking.builder()
                .id(UUID.randomUUID())
                .subFieldId(subField.getId())
                .subField(subField)
                .build();

        BookingResponse response = bookingMapper.toResponse(booking);

        assertEquals("Sân bóng Trung Tâm", response.getFieldName());
        assertEquals("Sân 5A", response.getSubFieldName());
    }
}
