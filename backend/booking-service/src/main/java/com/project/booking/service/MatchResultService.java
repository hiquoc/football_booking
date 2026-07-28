package com.project.booking.service;

import com.project.booking.dto.request.UpsertMatchResultRequest;
import com.project.booking.dto.response.BookingResponse;

import java.util.UUID;

public interface MatchResultService {
    BookingResponse upsert(UUID managerId, String managerRole, UUID bookingId, UpsertMatchResultRequest request);
}
